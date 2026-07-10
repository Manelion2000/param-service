import { createRequire } from 'node:module';
import path from 'node:path';

const require = createRequire('C:/eclipse-workspace/reconcillation-frontend/package.json');
const { chromium } = require('playwright');
const baseUrl = process.env.FRONTEND_URL ?? 'http://localhost:4200';
const outDir = path.resolve('docs/user-manual/screenshots');

const pageOf = (content) => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: 0,
  size: Math.max(content.length, 20)
});

const sampleSummary = {
  periodType: 'DAY',
  businessDate: '2026-04-23',
  channel: 'MOOV',
  totalBank: 423,
  totalOperator: 424,
  totalResults: 431,
  matchOk: 408,
  debitATort: 3,
  creditSansDebit: 2,
  echecDesDeuxCotes: 0,
  absentCoteOperateur: 8,
  absentCoteBanque: 16,
  montantDifferent: 4,
  statutInconnu: 0,
  doublons: 1,
  invalidRows: 0,
  matchingRate: 96.2,
  successRate: 98.1,
  anomalyRate: 3.8,
  montantGlobalBanque: 24213127,
  montantGlobalOperateur: 86962270,
  montantAnomalies: 540000,
  ecartGlobal: 62749143
};

const distribution = [
  { resultType: 'MATCH_OK', count: 408 },
  { resultType: 'ABSENT_COTE_BANQUE', count: 16 },
  { resultType: 'ABSENT_COTE_OPERATEUR', count: 8 },
  { resultType: 'MONTANT_DIFFERENT', count: 4 },
  { resultType: 'APPROVISIONNEMENT', count: 2 }
];

const anomalies = pageOf([
  {
    transactionKey: 'TXN-20260423-001',
    businessDate: '2026-04-23',
    resultType: 'ABSENT_COTE_BANQUE',
    operatorStatusRaw: 'SUCCESSFUL',
    operatorAmount: 150000,
    reason: 'Transaction aboutie operateur absente cote banque'
  },
  {
    transactionKey: 'TXN-20260423-014',
    businessDate: '2026-04-23',
    resultType: 'MONTANT_DIFFERENT',
    bankStatusRaw: 'SUCCESS',
    operatorStatusRaw: 'SUCCESSFUL',
    bankAmount: 75000,
    operatorAmount: 80000,
    amountDifference: 5000,
    reason: 'Montants differents entre les deux sources'
  }
]);

const reconciliationRows = pageOf([
  {
    id: 1,
    businessDate: '2026-04-23',
    transactionKey: 'TXN-20260423-001',
    resultType: 'MATCH_OK',
    bankStatusRaw: 'SUCCESS',
    moovStatusRaw: 'SUCCESSFUL',
    bankAmount: 25000,
    moovAmount: 25000,
    amountDifference: 0,
    reason: 'Operation rapprochee'
  },
  {
    id: 2,
    businessDate: '2026-04-23',
    transactionKey: 'TXN-20260423-014',
    resultType: 'ABSENT_COTE_BANQUE',
    bankStatusRaw: null,
    moovStatusRaw: 'SUCCESSFUL',
    bankAmount: null,
    moovAmount: 150000,
    amountDifference: 150000,
    reason: 'Aboutie operateur, absente banque'
  },
  {
    id: 3,
    businessDate: '2026-04-23',
    transactionKey: '203000000011127474',
    resultType: 'APPROVISIONNEMENT',
    bankStatusRaw: null,
    moovStatusRaw: 'SUCCESSFUL',
    bankAmount: null,
    moovAmount: 5000000,
    amountDifference: 0,
    reason: 'Approvisionnement ecarte du rapprochement financier'
  }
]);

const compensationRows = [
  {
    businessDate: '2026-04-23',
    operator: 'MOOV',
    operatorSuccessCount: 424,
    bankSuccessCount: 423,
    operatorSuccessAmount: 86962270,
    bankSuccessAmount: 24213127,
    difference: 62749143,
    decision: 'A_VERIFIER'
  },
  {
    businessDate: '2026-04-24',
    operator: 'MOOV',
    operatorSuccessCount: 391,
    bankSuccessCount: 391,
    operatorSuccessAmount: 30650000,
    bankSuccessAmount: 30650000,
    difference: 0,
    decision: 'OK_COMPENSATION'
  }
];

const accountingRows = [
  {
    transactionId: 'TXN-20260423-001',
    operationDate: '2026-04-23',
    amount: 25000,
    amplitudeCredit: 25000,
    accountNumber: '25110000001',
    bankPhoneNumber: '70000001',
    operationReference: 'OP-001',
    pieceNumber: 'PCE-001',
    eventNumber: 'EVT-001',
    operationNature: 'BANK_TO_WALLET',
    status: 'COMPTABILISE'
  },
  {
    transactionId: 'TXN-20260423-017',
    operationDate: '2026-04-23',
    amount: 150000,
    amplitudeCredit: null,
    accountNumber: '25110000002',
    bankPhoneNumber: '70000002',
    operationReference: 'OP-017',
    pieceNumber: null,
    eventNumber: null,
    operationNature: 'WALLET_TO_BANK',
    status: 'NON_COMPTABILISE'
  }
];

function apiResponse(url) {
  const pathname = new URL(url).pathname;
  if (pathname.endsWith('/users/details')) return { login: 'manuel.user@example.com', firstName: 'Utilisateur', lastName: 'Manuel' };
  if (pathname.includes('/dashboard/reconciliation/summary')) return sampleSummary;
  if (pathname.includes('/dashboard/reconciliation/results-distribution')) return distribution;
  if (pathname.includes('/dashboard/reconciliation/amounts')) return {
    montantGlobalBanque: sampleSummary.montantGlobalBanque,
    montantGlobalOperateur: sampleSummary.montantGlobalOperateur,
    montantAnomalies: sampleSummary.montantAnomalies,
    ecartGlobal: sampleSummary.ecartGlobal
  };
  if (pathname.includes('/dashboard/reconciliation/timeline')) return [
    { hour: '08:00', totalTransactions: 52, anomalies: 2 },
    { hour: '10:00', totalTransactions: 88, anomalies: 3 },
    { hour: '12:00', totalTransactions: 110, anomalies: 5 }
  ];
  if (pathname.includes('/dashboard/reconciliation/top-anomalies')) return anomalies;
  if (pathname.includes('/dashboard/reconciliation/data-quality')) return {
    totalImports: 4,
    invalidRows: 0,
    validRows: 1258,
    parsingSuccessRate: 100,
    duplicateCount: 1,
    duplicateRate: 0.08,
    invalidRowRate: 0
  };
  if (pathname.includes('/dashboard/reconciliation/reporting/summary')) return {
    periodType: 'DAY',
    referenceDate: '2026-04-23',
    dateFrom: '2026-04-23',
    dateTo: '2026-04-23',
    channel: 'MOOV',
    generatedAt: '2026-07-09T10:00:00',
    kpis: {
      totalTransactions: 431,
      matchingCount: 408,
      anomalyCount: 23,
      successRate: 94.7,
      anomalyRate: 5.3,
      debitATortCount: 3,
      creditSansDebitCount: 2,
      absentCoteBanqueCount: 16,
      absentCoteOperateurCount: 8,
      montantDifferentCount: 4,
      doublonsCount: 1,
      statutInconnuCount: 0,
      montantTotalBanque: 24213127,
      montantTotalOperateur: 86962270,
      montantAnomalies: 540000,
      ecartGlobal: 62749143,
      operateurSuccessCount: 424,
      operateurSuccessAmount: 86962270,
      bankSuccessCount: 423,
      bankSuccessAmount: 24213127,
      operateurSuccessSansCarthagoCount: 16,
      operateurSuccessSansCarthagoAmount: 2500000,
      operateurHorsPerimetreCount: 2,
      operateurHorsPerimetreAmount: 10000000,
      moyenneJournaliereTransactions: 431,
      picVolumeJournalier: { businessDate: '2026-04-23', totalTransactions: 431 }
    },
    distribution,
    dailyBreakdown: [{ businessDate: '2026-04-23', totalTransactions: 431, matchingCount: 408, anomalyCount: 23, successRate: 94.7, montantBanque: 24213127, montantOperateur: 86962270, ecart: 62749143 }],
    transactionDetails: []
  };
  if (pathname.includes('/reconciliations/history/latest')) return { id: 1, label: 'Run demonstration', operator: 'MOOV', businessDateFrom: '2026-04-23', businessDateTo: '2026-04-23', startedAt: '2026-07-09T10:00:00', status: 'FINISHED' };
  if (pathname.includes('/reconciliations/summary')) return { ...sampleSummary, totalMoov: 424, totalAbsentMoov: 8, totalAbsentBanque: 16, totalDebitATort: 3, totalCreditSansDebit: 2, totalMontantDifferent: 4, totalDoublons: 1, totalMatchOk: 408, montantGlobalMoov: 86962270, ecartGlobal: 62749143 };
  if (pathname.includes('/reconciliations/results')) return reconciliationRows;
  if (pathname.includes('/reconciliations/runs')) return pageOf([{ id: 1, label: 'Run demonstration', operator: 'MOOV', businessDateFrom: '2026-04-23', businessDateTo: '2026-04-23', startedAt: '2026-07-09T10:00:00', status: 'FINISHED' }]);
  if (pathname.includes('/compensations/daily')) return compensationRows;
  if (pathname.includes('/compensations/weekly') || pathname.includes('/compensations/monthly')) return {
    periodType: 'WEEK',
    label: 'Semaine de demonstration',
    rows: compensationRows,
    totalOperatorSuccessCount: 815,
    totalBankSuccessCount: 814,
    totalOperatorSuccessAmount: 117612270,
    totalBankSuccessAmount: 54863127,
    totalDifference: 62749143,
    decision: 'A_VERIFIER'
  };
  if (pathname.includes('/compensations/discrepancies')) return reconciliationRows;
  if (pathname.includes('/accounting') && pathname.endsWith('/kpi')) return {
    totalTransactions: 423,
    totalAmount: 24213127,
    comptabilizedTransactions: 421,
    comptabilizedAmount: 24063127,
    comptabilizationRateCount: 99.5,
    comptabilizationRateAmount: 99.4,
    nonComptabilizedTransactions: 2,
    nonComptabilizedAmount: 150000,
    nonComptabilizationRateCount: 0.5,
    amountAtRisk: 150000,
    netGapAmount: 150000,
    absoluteGapAmount: 150000,
    transactionsWithGapCount: 2,
    totalGapAmount: 150000
  };
  if (pathname.includes('/accounting') && pathname.endsWith('/check')) return accountingRows;
  if (pathname.includes('/accounting/carthago/latest-date')) return '2026-04-23';
  return pageOf([]);
}

async function screenshotRoute(page, route, fileName) {
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(700);
  await page.screenshot({ path: path.join(outDir, fileName), fullPage: true });
}

async function screenshotCurrent(page, fileName) {
  await page.waitForTimeout(700);
  await page.screenshot({ path: path.join(outDir, fileName), fullPage: true });
}

const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext({ viewport: { width: 1440, height: 1200 }, deviceScaleFactor: 1 });
const page = await context.newPage();

await page.route('**/api/**', async (route) => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(apiResponse(route.request().url()))
  });
});

await page.goto(baseUrl, { waitUntil: 'networkidle' });
await page.screenshot({ path: path.join(outDir, '01-connexion.png'), fullPage: true });
await page.getByText(/creer un compte/i).click().catch(() => {});
await page.waitForTimeout(400);
await page.screenshot({ path: path.join(outDir, '02-inscription.png'), fullPage: true });

await page.evaluate(() => {
  localStorage.setItem('reconciliation_access_token', 'manual-demo-token');
  localStorage.setItem('reconciliation_current_user', JSON.stringify({ username: 'manuel.user@example.com', email: 'manuel.user@example.com', nom: 'MANUEL', prenom: 'UTILISATEUR' }));
});

await screenshotRoute(page, '/home', '03-accueil.png');
await page.locator('.home-operator-card').first().click();
await screenshotCurrent(page, '05-reconciliation.png');
await page.getByRole('button', { name: /^Dashboard$/ }).click();
await screenshotCurrent(page, '04-dashboard.png');
await page.getByRole('button', { name: /^Compensation$/ }).click();
await screenshotCurrent(page, '06-compensation.png');
await page.getByRole('button', { name: /^Comptabilisation$/ }).click();
await screenshotCurrent(page, '07-comptabilisation.png');

await browser.close();
