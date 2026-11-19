export type Language = 'ar' | 'en';
export type ThemeMode = 'light' | 'dark';

export type UIStateMode = 'populated' | 'loading' | 'empty' | 'success' | 'error';

export type BusinessTypeId =
  | 'retail'
  | 'restaurant'
  | 'pharmacy'
  | 'workshop'
  | 'factory'
  | 'services';

export type ScreenFlowGroup =
  | 'onboarding'
  | 'workspace'
  | 'accounting'
  | 'commerce'
  | 'specialized'
  | 'governance';

export type ScreenId =
  // 1. Onboarding, Isolation & Auth
  | 'splash'
  | 'business-type'
  | 'create-activity'
  | 'login'
  | 'restore'
  // 2. Command & Hub
  | 'dashboard'
  | 'main-menu'
  | 'notifications'
  // 3. Core Double-Entry Accounting & Statements
  | 'accounts'
  | 'journal-entries'
  | 'general-ledger'
  | 'treasury-banks'
  | 'expenses-revenues'
  | 'fixed-assets'
  | 'trial-balance'
  | 'financial-statements'
  | 'cash-flows'
  // 4. Commerce, Operations & HR
  | 'sales'
  | 'create-invoice'
  | 'pos-terminal'
  | 'purchases'
  | 'parties'
  | 'inventory'
  | 'barcode-qr'
  | 'employees'
  | 'budgets'
  | 'attachments'
  // 5. Specialized Sector Modules
  | 'branches'
  | 'projects'
  | 'manufacturing'
  | 'restaurants'
  | 'pharmacies'
  | 'workshops'
  | 'shipping'
  | 'loyalty'
  | 'taxes-currencies'
  // 6. Reports, Audit, Vault & Offline AI
  | 'reports'
  | 'ai-assistant'
  | 'backup'
  | 'security'
  | 'settings';

export interface BilingualText {
  ar: string;
  en: string;
}

export interface ScreenSpec {
  id: ScreenId;
  order: number;
  group: ScreenFlowGroup;
  title: BilingualText;
  subtitle: BilingualText;
  composeRoute: string;
  purpose: BilingualText;
  keyElements: { ar: string[]; en: string[] };
  nextScreens: ScreenId[];
  moduleNumbers: number[];
  sectorOnly?: BusinessTypeId[];
}

export interface ModuleItem {
  number: number;
  id: string;
  name: BilingualText;
  layer: 'core' | 'operations' | 'specialized' | 'governance';
  alwaysEnabled: boolean;
  businessTypes?: BusinessTypeId[];
  targetScreen: ScreenId;
}

export interface BusinessTypeSpec {
  id: BusinessTypeId;
  name: BilingualText;
  activityName: BilingualText;
  dbFileName: string;
  summary: BilingualText;
  defaultCurrency: string;
  fiscalStart: string;
  specializedModules: number[];
}

export interface ColorToken {
  id: string;
  name: BilingualText;
  composeToken: string;
  lightHex: string;
  darkHex: string;
  usage: BilingualText;
  role: 'primary' | 'accent' | 'semantic' | 'neutral';
}
