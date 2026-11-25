const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_uid_123";
const CASHIER_UID = "cashier_uid_456";
const STRANGER_UID = "stranger_uid_789";
const STORE_ID = OWNER_UID;
const JOIN_CODE = "TB48291A";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validStorePayload(created, updated) {
  return {
    ownerUid: OWNER_UID,
    memberUids: [OWNER_UID],
    joinCode: JOIN_CODE,
    storeName: "TB PUTRA RI",
    storeAddress: "Jl. Raya Pembangunan No. 88",
    storePhone: "0812-3456-7890",
    ownerName: "H. Putra",
    cashierName: "Budi Santoso",
    ownerUsername: "owner",
    ownerPassword: "owner123",
    cashierUsername: "kasir",
    cashierPassword: "kasir123",
    createdAt: created,
    updatedAt: updated,
  };
}

function validProductPayload(created, updated, members = [OWNER_UID]) {
  return {
    storeId: STORE_ID,
    ownerUid: OWNER_UID,
    memberUids: members,
    clientNumericId: 1001,
    name: "Semen Tiga Roda 50kg",
    category: "Semen",
    baseUnit: "Sak",
    stockQuantity: 85.0,
    minStock: 20.0,
    costPricePerBaseUnit: 57000,
    sellPricePerBaseUnit: 65000,
    minSellPricePerBaseUnit: 62000,
    retailUnitName: "Kg",
    retailConversionFactor: 0.02,
    retailSellPrice: 1500,
    minRetailSellPrice: 1300,
    retailStockQuantity: 50.0,
    updatedAtMillis: Date.now(),
    createdAt: created,
    updatedAt: updated,
  };
}

function validTransactionPayload(created, updated, uid = OWNER_UID, members = [OWNER_UID]) {
  return {
    storeId: STORE_ID,
    ownerUid: OWNER_UID,
    recordedByUid: uid,
    memberUids: members,
    clientNumericId: 2001,
    invoiceNumber: "INV-PR-2401",
    timestampMillis: Date.now() - 1000,
    cashierName: "H. Putra",
    cashierRole: "OWNER",
    customerType: "Kontraktor",
    customerName: "Pak Mandor Slamet",
    customerPhone: "081398765432",
    paymentMethod: "Tempo / Kasbon",
    subtotal: 930000,
    discountAmount: 46500,
    totalAmount: 883500,
    totalCost: 790000,
    amountPaid: 0,
    changeAmount: 0,
    paidStatus: false,
    itemsJson: "[]",
    createdAt: created,
    updatedAt: updated,
  };
}

function validExpensePayload(created, updated, uid = CASHIER_UID, members = [OWNER_UID, CASHIER_UID]) {
  return {
    storeId: STORE_ID,
    ownerUid: OWNER_UID,
    recordedByUid: uid,
    memberUids: members,
    clientNumericId: 3001,
    title: "Isi Solar Pikap L300",
    category: "BBM Armada",
    amount: 100000,
    notes: "Antar pesanan",
    recordedBy: "Budi Santoso",
    recordedRole: "CASHIER",
    timestampMillis: Date.now() - 1000,
    createdAt: created,
    updatedAt: updated,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated requests are rejected across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(OWNER_UID).get());
  await assertFails(unauthDb.collection("storeInvites").doc(JOIN_CODE).get());
  await assertFails(unauthDb.collection("stores").where("memberUids", "array-contains", OWNER_UID).get());
  await assertFails(
    unauthDb.collection("stores").doc(STORE_ID).collection("products").where("memberUids", "array-contains", OWNER_UID).get()
  );
});

test("User workspace pointer isolation and schema bounds", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    ownerDb.collection("users").doc(OWNER_UID).set({
      userId: OWNER_UID,
      defaultStoreId: STORE_ID,
      displayName: "H. Putra",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Stranger cannot read or spoof owner workspace
  await assertFails(strangerDb.collection("users").doc(OWNER_UID).get());
  await assertFails(
    strangerDb.collection("users").doc(OWNER_UID).set({
      userId: OWNER_UID,
      defaultStoreId: "hijack",
      displayName: "Hacker",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Reject future timestamp
  await assertFails(
    strangerDb.collection("users").doc(STRANGER_UID).set({
      userId: STRANGER_UID,
      defaultStoreId: STRANGER_UID,
      displayName: "Future User",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: ts,
    })
  );
});

test("Store creation, invite lookup, cashier join flow, and subcollection queries", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const cashierDb = testEnv.authenticatedContext(CASHIER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Owner creates store and invite
  await assertSucceeds(ownerDb.collection("stores").doc(STORE_ID).set(validStorePayload(ts, ts)));
  await assertSucceeds(
    ownerDb.collection("storeInvites").doc(JOIN_CODE).set({
      joinCode: JOIN_CODE,
      storeId: STORE_ID,
      ownerUid: OWNER_UID,
      storeName: "TB PUTRA RI",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invite listing is strictly forbidden; single doc lookup with exact code succeeds
  await assertFails(cashierDb.collection("storeInvites").get());
  await assertSucceeds(cashierDb.collection("storeInvites").doc(JOIN_CODE).get());

  // Stranger cannot hijack store settings
  await assertFails(
    strangerDb.collection("stores").doc(STORE_ID).update({
      ownerPin: "9999",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cashier joins store by adding only their UID
  await assertSucceeds(
    cashierDb.collection("stores").doc(STORE_ID).update({
      memberUids: [OWNER_UID, CASHIER_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Unfiltered list on stores or products fails; filtered list succeeds (even on empty collection)
  await assertFails(ownerDb.collection("stores").get());
  await assertSucceeds(
    ownerDb.collection("stores").where("memberUids", "array-contains", OWNER_UID).get()
  );
  await assertFails(ownerDb.collection("stores").doc(STORE_ID).collection("products").get());
  await assertSucceeds(
    cashierDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("products")
      .where("memberUids", "array-contains", CASHIER_UID)
      .get()
  );
});

test("Products, transactions, kasbon settlement, and cashier privilege separation", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const cashierDb = testEnv.authenticatedContext(CASHIER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const ts = new Date(Date.now() - 10000);
  const members = [OWNER_UID, CASHIER_UID];

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("stores").doc(STORE_ID).set({
      ...validStorePayload(ts, ts),
      memberUids: members,
    });
  });

  const prodRef = ownerDb.collection("stores").doc(STORE_ID).collection("products").doc("prod_1001");
  await assertSucceeds(prodRef.set(validProductPayload(ts, ts, members)));

  // Reject ghost field on create
  await assertFails(
    ownerDb.collection("stores").doc(STORE_ID).collection("products").doc("prod_ghost").set({
      ...validProductPayload(ts, ts, members),
      ghostField: "bad",
    })
  );

  // Cashier can deduct base stock and retail stock on checkout
  const cashierProdRef = cashierDb.collection("stores").doc(STORE_ID).collection("products").doc("prod_1001");
  await assertSucceeds(
    cashierProdRef.update({
      stockQuantity: 80.0,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    cashierProdRef.update({
      retailStockQuantity: 40.0,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cashier can repack base stock (80 -> 79 Sak) into separated retail stock (40 -> 90 Kg)
  await assertSucceeds(
    cashierProdRef.update({
      stockQuantity: 79.0,
      retailStockQuantity: 90.0,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cashier CANNOT inflate retail stock beyond conversion factor or without reducing base stock
  await assertFails(
    cashierProdRef.update({
      stockQuantity: 78.0,
      retailStockQuantity: 250.0,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    cashierProdRef.update({
      retailStockQuantity: 100.0,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cashier CANNOT increase stock, change price, or delete product
  await assertFails(
    cashierProdRef.update({
      stockQuantity: 100.0,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    cashierProdRef.update({
      sellPricePerBaseUnit: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    cashierProdRef.update({
      minSellPricePerBaseUnit: 1000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(cashierProdRef.delete());

  // Owner cannot set minimum price higher than standard sell price
  await assertFails(
    prodRef.update({
      minSellPricePerBaseUnit: 99000,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    prodRef.update({
      minRetailSellPrice: 9000,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    prodRef.update({
      minSellPricePerBaseUnit: 60000,
      minRetailSellPrice: 1200,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Stranger cannot read or mutate product
  await assertFails(
    strangerDb.collection("stores").doc(STORE_ID).collection("products").doc("prod_1001").get()
  );

  // Kasbon transaction lifecycle, partial installment & terminal lock
  const txRef = ownerDb.collection("stores").doc(STORE_ID).collection("transactions").doc("tx_2001");
  await assertSucceeds(txRef.set(validTransactionPayload(ts, ts, OWNER_UID, members)));

  // Stranger cannot mark kasbon paid or record installment
  await assertFails(
    strangerDb.collection("stores").doc(STORE_ID).collection("transactions").doc("tx_2001").update({
      paidStatus: false,
      paidAtMillis: Date.now(),
      amountPaid: 200000,
      updatedAt: new Date(Date.now() - 1500),
    })
  );
  // Owner/member cannot mark paidStatus=true when amountPaid < totalAmount
  await assertFails(
    txRef.update({
      paidStatus: true,
      paidAtMillis: Date.now(),
      amountPaid: 300000,
      updatedAt: new Date(Date.now() - 1400),
    })
  );
  // Cashier (store member) can record partial installment with paymentsJson
  await assertSucceeds(
    cashierDb.collection("stores").doc(STORE_ID).collection("transactions").doc("tx_2001").update({
      paidStatus: false,
      paidAtMillis: Date.now(),
      amountPaid: 300000,
      paymentMethod: "Tempo / Kasbon",
      paymentsJson: '[{"amount":300000,"paymentMethod":"Tunai","timestamp":1700000000,"recordedBy":"Budi Santoso"}]',
      updatedAt: new Date(Date.now() - 1200),
    })
  );
  // Owner can settle remaining balance via Transfer/QRIS
  await assertSucceeds(
    txRef.update({
      paidStatus: true,
      paidAtMillis: Date.now(),
      amountPaid: 883500,
      paymentMethod: "Transfer/QRIS (Kasbon)",
      paymentsJson: '[{"amount":300000,"paymentMethod":"Tunai","timestamp":1700000000,"recordedBy":"H. Putra"},{"amount":583500,"paymentMethod":"Transfer/QRIS","timestamp":1700001000,"recordedBy":"H. Putra"}]',
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Terminal lock: cannot flip paidStatus back to false
  await assertFails(
    txRef.update({
      paidStatus: false,
      paidAtMillis: null,
      amountPaid: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cashier can record expense
  const expRef = cashierDb.collection("stores").doc(STORE_ID).collection("expenses").doc("exp_3001");
  await assertSucceeds(expRef.set(validExpensePayload(ts, ts, CASHIER_UID, members)));
  // Cashier cannot delete expense; Owner can
  await assertFails(expRef.delete());
  await assertSucceeds(ownerDb.collection("stores").doc(STORE_ID).collection("expenses").doc("exp_3001").delete());

  // Owner can update store credentials & address, and update product details (including omitting retailUnitName)
  await assertSucceeds(
    ownerDb.collection("stores").doc(STORE_ID).update({
      ownerUsername: "putra_owner",
      ownerPassword: "passwordBaru123",
      cashierUsername: "budi_kasir",
      cashierPassword: "kasirBaru123",
      storeAddress: "Kresek, Tangerang",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Reject invalid short username
  await assertFails(
    ownerDb.collection("stores").doc(STORE_ID).update({
      ownerUsername: "ab",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    prodRef.update({
      sellPricePerBaseUnit: 68000,
      minSellPricePerBaseUnit: 63000,
      retailUnitName: null,
      retailSellPrice: 0,
      minRetailSellPrice: 0,
      updatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Employee management: owner can create, update, delete; cashier can read list and update profile/credentials
  const empRef = ownerDb.collection("stores").doc(STORE_ID).collection("employees").doc("emp_1");
  await assertSucceeds(
    empRef.set({
      storeId: STORE_ID,
      ownerUid: OWNER_UID,
      memberUids: members,
      name: "Siti Rahma",
      username: "siti_kasir",
      password: "password123",
      phone: "0812-1111-2222",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    cashierDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("employees")
      .where("memberUids", "array-contains", CASHIER_UID)
      .get()
  );
  // Cashier cannot create, delete, or tamper with employee memberUids
  await assertFails(
    cashierDb.collection("stores").doc(STORE_ID).collection("employees").doc("emp_2").set({
      storeId: STORE_ID,
      ownerUid: OWNER_UID,
      memberUids: members,
      name: "Rogue",
      username: "rogue",
      password: "password123",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    cashierDb.collection("stores").doc(STORE_ID).collection("employees").doc("emp_1").update({
      memberUids: [OWNER_UID],
      updatedAt: new Date(Date.now() - 250),
    })
  );
  await assertFails(
    cashierDb.collection("stores").doc(STORE_ID).collection("employees").doc("emp_1").delete()
  );
  // Cashier (store member) can edit employee profile, phone, username, and password
  await assertSucceeds(
    cashierDb.collection("stores").doc(STORE_ID).collection("employees").doc("emp_1").update({
      name: "Siti Rahma Putri",
      username: "siti_kasir_baru",
      password: "passBaru123",
      phone: "0813-9999-0000",
      updatedAt: new Date(Date.now() - 220),
    })
  );
  // Owner can edit and delete employee
  await assertSucceeds(
    empRef.update({
      name: "Siti R.",
      username: "siti_baru",
      password: "newpass123",
      phone: "0812-3333-4444",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertSucceeds(empRef.delete());

  // Category CRUD: owner can create, edit, delete; cashier can read list but cannot mutate
  const catRef = ownerDb.collection("stores").doc(STORE_ID).collection("categories").doc("cat_1");
  await assertSucceeds(
    catRef.set({
      storeId: STORE_ID,
      ownerUid: OWNER_UID,
      memberUids: members,
      name: "Keramik & Granit",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    cashierDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("categories")
      .where("memberUids", "array-contains", CASHIER_UID)
      .get()
  );
  await assertFails(
    cashierDb.collection("stores").doc(STORE_ID).collection("categories").doc("cat_2").set({
      storeId: STORE_ID,
      ownerUid: OWNER_UID,
      memberUids: members,
      name: "Illegal Category",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    catRef.update({
      name: "",
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    cashierDb.collection("stores").doc(STORE_ID).collection("categories").doc("cat_1").delete()
  );
  await assertSucceeds(
    catRef.update({
      name: "Keramik & Marmer",
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertSucceeds(catRef.delete());
});
