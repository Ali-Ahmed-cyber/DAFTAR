import React, { useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  BarChart3,
  Bell,
  BookOpen,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  FilePlus2,
  Fingerprint,
  FolderOpen,
  Home,
  KeyRound,
  LayoutGrid,
  Lock,
  Minus,
  Plus,
  QrCode,
  Receipt,
  ShieldCheck,
  Sparkles,
  Store,
} from 'lucide-react';
import { BUSINESS_TYPES, MODULES_37, SCREENS_41 } from '../data/daftarSpec';
import { BusinessTypeId, Language, ScreenId, ThemeMode, UIStateMode } from '../types/daftar';
import { DaftarLogo } from './DaftarLogo';
import { renderExtraMobileScreen } from './PhoneScreensExtra';

interface PhonePrototypeFrameProps {
  lang: Language;
  theme: ThemeMode;
  activeScreen: ScreenId;
  activeBusinessType: BusinessTypeId;
  uiState: UIStateMode;
  onChangeUiState: (state: UIStateMode) => void;
  onNavigate: (screen: ScreenId) => void;
  onBack: () => void;
  onSelectBusinessType: (type: BusinessTypeId) => void;
  onToggleLang: () => void;
  onToggleTheme: () => void;
}

export const PhonePrototypeFrame: React.FC<PhonePrototypeFrameProps> = ({
  lang,
  theme,
  activeScreen,
  activeBusinessType,
  uiState,
  onChangeUiState,
  onNavigate,
  onBack,
  onSelectBusinessType,
  onToggleLang,
  onToggleTheme,
}) => {
  const isAr = lang === 'ar';
  const BackIcon = isAr ? ArrowRight : ArrowLeft;
  const ForwardIcon = isAr ? ChevronLeft : ChevronRight;

  // Interactive prototype states
  const [pinDigits, setPinDigits] = useState<string>('12');
  const [pinError, setPinError] = useState<string>('');
  const [selectedSalesFilter, setSelectedSalesFilter] = useState<'all' | 'paid' | 'credit'>('all');
  const [selectedPartyTab, setSelectedPartyTab] = useState<'customers' | 'suppliers'>('customers');
  const [statementTab, setStatementTab] = useState<'pnl' | 'bs'>('pnl');
  const [invoiceQty, setInvoiceQty] = useState<number>(2);
  const [invoicePosted, setInvoicePosted] = useState<boolean>(false);
  const [posCartCount, setPosCartCount] = useState<number>(3);
  const [auditRan, setAuditRan] = useState<boolean>(false);
  const [backupVerified, setBackupVerified] = useState<boolean>(false);
  const [restoreChecked, setRestoreChecked] = useState<boolean>(false);
  const [showRecoveryKey, setShowRecoveryKey] = useState<boolean>(false);
  const [aiQueryIndex, setAiQueryIndex] = useState<number>(0);

  const currentBusiness =
    BUSINESS_TYPES.find((b) => b.id === activeBusinessType) || BUSINESS_TYPES[0];
  const currentScreenSpec = SCREENS_41.find((s) => s.id === activeScreen) || SCREENS_41[0];
  const cur = currentBusiness.defaultCurrency;

  const activeModules = MODULES_37.filter(
    (m) => m.alwaysEnabled || (m.businessTypes && m.businessTypes.includes(activeBusinessType))
  );

  // Calculations for Interactive Invoice Builder
  const unitPrice = 500;
  const subtotal = invoiceQty * unitPrice;
  const vatAmount = +(subtotal * 0.15).toFixed(2);
  const totalAmount = +(subtotal + vatAmount).toFixed(2);

  const handlePinKey = (digit: string) => {
    setPinError('');
    if (digit === 'clear') {
      setPinDigits('');
      return;
    }
    if (digit === 'back') {
      setPinDigits((prev) => prev.slice(0, -1));
      return;
    }
    if (pinDigits.length >= 4) return;
    const next = pinDigits + digit;
    setPinDigits(next);
    if (next.length === 4) {
      if (next === '1234' || next === '0000') {
        setTimeout(() => {
          setPinDigits('');
          onNavigate('dashboard');
        }, 160);
      } else {
        setPinError(
          isAr
            ? 'رمز غير صحيح (جرّب 1234 للمعاينة) · تم تسجيل المحاولة'
            : 'Invalid PIN (use 1234 for preview) · Logged to Audit'
        );
      }
    }
  };

  const showBottomBar = !['splash', 'business-type', 'create-activity', 'login', 'restore'].includes(
    activeScreen
  );

  const renderPrimaryScreens = (): React.ReactNode => {
    switch (activeScreen) {
      case 'splash':
        return (
          <div className="h-full bg-[#0B192C] text-white flex flex-col justify-between p-6 text-center">
            <div className="pt-6 flex flex-col items-center">
              <DaftarLogo variant="gold-on-navy" size={80} />
              <h3 className="text-2xl font-extrabold mt-4 tracking-tight">
                دفتر <span className="text-[#C89D42] mx-1">|</span> DAFTAR
              </h3>
              <p className="text-xs text-slate-300 mt-1">
                {isAr ? 'المحاسب الذكي · يعمل دون إنترنت' : 'Smart Accounting · Offline-First'}
              </p>
            </div>

            <div className="space-y-2 text-start bg-[#132642] border border-white/10 rounded-2xl p-4">
              <div className="flex items-center gap-2 text-xs text-emerald-400 font-semibold">
                <ShieldCheck className="w-4 h-4 shrink-0" />
                <span>{isAr ? 'حافظة معزولة مشفرة محلياً' : 'Isolated Encrypted Local Vault'}</span>
              </div>
              <p className="text-[11px] text-slate-300 leading-relaxed">
                {currentBusiness.activityName[lang]} ·{' '}
                <span className="font-mono-num text-[#D9B15B]">{currentBusiness.dbFileName}</span>
              </p>
            </div>

            <div className="space-y-2 pb-1">
              <button
                type="button"
                onClick={() => onNavigate('login')}
                className="w-full min-h-[46px] rounded-xl bg-[#C89D42] text-[#0B192C] font-bold text-xs flex items-center justify-center gap-2"
              >
                <Lock className="w-4 h-4" />
                <span>{isAr ? 'تسجيل الدخول للنشاط' : 'Unlock Business Vault'}</span>
              </button>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => onNavigate('business-type')}
                  className="min-h-[42px] rounded-xl border border-white/15 text-slate-100 text-[11px] font-semibold"
                >
                  {isAr ? 'إنشاء نشاط جديد' : 'New Activity'}
                </button>
                <button
                  type="button"
                  onClick={() => onNavigate('restore')}
                  className="min-h-[42px] rounded-xl border border-white/15 text-slate-100 text-[11px] font-semibold"
                >
                  {isAr ? 'استعادة نسخة' : 'Restore Backup'}
                </button>
              </div>
            </div>
          </div>
        );

      case 'business-type':
        return (
          <div className="p-4 space-y-3">
            <div className="text-xs text-slate-500 dark:text-slate-400">
              {isAr
                ? 'اختر نوع النشاط لتفعيل الوحدات القطاعية المناسبة وعزل قاعدة بياناته:'
                : 'Select sector to activate dedicated modules and isolate its database:'}
            </div>
            <div className="space-y-2">
              {BUSINESS_TYPES.map((type) => {
                const selected = type.id === activeBusinessType;
                return (
                  <button
                    key={type.id}
                    type="button"
                    onClick={() => onSelectBusinessType(type.id)}
                    className={`w-full text-start p-3 rounded-xl border transition-colors ${
                      selected
                        ? 'border-[#C89D42] bg-[#C89D42]/10 dark:bg-[#C89D42]/15'
                        : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33]'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs font-bold">{type.name[lang]}</span>
                      <span className="font-mono-num text-[10px] text-[#9A7222] dark:text-[#D9B15B]">
                        {type.defaultCurrency}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-600 dark:text-slate-400 mt-1">
                      {type.summary[lang]}
                    </p>
                  </button>
                );
              })}
            </div>
            <button
              type="button"
              onClick={() => onNavigate('create-activity')}
              className="w-full min-h-[44px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold"
            >
              {isAr ? 'التالي: إنشاء النشاط المستقل' : 'Next: Create Isolated Activity'}
            </button>
          </div>
        );

      case 'create-activity':
        return (
          <div className="p-4 space-y-3">
            <div className="p-3 rounded-xl bg-[#0B192C] text-white text-xs space-y-1">
              <div className="text-[#D9B15B] font-bold">
                {currentBusiness.name[lang]} · {currentBusiness.dbFileName}
              </div>
              <p className="text-[11px] text-slate-300">
                {isAr
                  ? 'مستودعات وحسابات وعملة وسنة مالية ونسخ احتياطية مستقلة بالكامل.'
                  : 'Completely independent warehouses, accounts, currency, fiscal year & backups.'}
              </p>
            </div>
            <div className="space-y-2 text-xs">
              <div className="p-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#0F1E33]">
                <span className="text-[10px] text-slate-500 block">{isAr ? 'اسم النشاط' : 'Activity Name'}</span>
                <span className="font-bold">{currentBusiness.activityName[lang]}</span>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div className="p-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#0F1E33]">
                  <span className="text-[10px] text-slate-500 block">{isAr ? 'العملة الأساسية' : 'Currency'}</span>
                  <span className="font-mono-num font-bold">{cur}</span>
                </div>
                <div className="p-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#0F1E33]">
                  <span className="text-[10px] text-slate-500 block">{isAr ? 'السنة المالية' : 'Fiscal Start'}</span>
                  <span className="font-mono-num font-bold">{currentBusiness.fiscalStart}</span>
                </div>
              </div>
              <div className="p-3 rounded-xl border border-amber-200 dark:border-amber-900/60 bg-amber-50/70 dark:bg-amber-950/20">
                <div className="flex items-center gap-1.5 text-[11px] font-bold text-[#9A7222] dark:text-[#D9B15B]">
                  <KeyRound className="w-3.5 h-3.5" />
                  <span>{isAr ? 'مفتاح الاسترداد الخارجي' : 'External Recovery Key'}</span>
                </div>
                <div className="font-mono-num text-[11px] mt-1">DFTR-9F4A-88C2-7E10-B3A9</div>
              </div>
            </div>
            <button
              type="button"
              onClick={() => onNavigate('dashboard')}
              className="w-full min-h-[44px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold"
            >
              {isAr ? 'تهيئة الحافظة والدخول' : 'Provision Vault & Enter'}
            </button>
          </div>
        );

      case 'login':
        return (
          <div className="p-4 flex flex-col justify-between h-full">
            <div className="space-y-3">
              <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-lg bg-[#0B192C] text-[#C89D42] flex items-center justify-center">
                    <Store className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-bold">{currentBusiness.activityName[lang]}</div>
                    <div className="font-mono-num text-[10px] text-slate-500">{currentBusiness.dbFileName}</div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => onNavigate('business-type')}
                  className="text-[11px] font-bold text-[#9A7222] dark:text-[#D9B15B]"
                >
                  {isAr ? 'تبديل' : 'Switch'}
                </button>
              </div>
              <div className="text-center pt-1">
                <p className="text-xs font-semibold">{isAr ? 'أدخل PIN المستخدم (1234 للمعاينة)' : 'Enter User PIN (1234 to unlock)'}</p>
                <div className="flex items-center justify-center gap-3 my-2.5">
                  {[0, 1, 2, 3].map((idx) => (
                    <div
                      key={idx}
                      className={`w-3.5 h-3.5 rounded-full border ${
                        idx < pinDigits.length ? 'bg-[#C89D42] border-[#C89D42]' : 'border-slate-300 dark:border-slate-700'
                      }`}
                    />
                  ))}
                </div>
                {pinError && <p className="text-[10px] text-[#DC2626] dark:text-[#F87171]">{pinError}</p>}
              </div>
            </div>
            <div className="grid grid-cols-3 gap-2 my-1" dir="ltr">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9', 'clear', '0', 'back'].map((k) => (
                <button
                  key={k}
                  type="button"
                  onClick={() => handlePinKey(k)}
                  className="min-h-[42px] rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] text-sm font-bold font-mono-num active:scale-95 transition-transform"
                >
                  {k === 'clear' ? 'C' : k === 'back' ? '⌫' : k}
                </button>
              ))}
            </div>
            <div className="flex items-center justify-between pt-2 border-t border-slate-200 dark:border-slate-800">
              <button type="button" onClick={() => onNavigate('dashboard')} className="flex items-center gap-1.5 text-xs font-bold text-[#9A7222] dark:text-[#D9B15B] min-h-[38px]">
                <Fingerprint className="w-4 h-4" />
                <span>{isAr ? 'البصمة' : 'Biometric'}</span>
              </button>
              <button type="button" onClick={() => onNavigate('restore')} className="text-xs text-slate-500 min-h-[38px]">
                {isAr ? 'استعادة نسخة' : 'Restore'}
              </button>
            </div>
          </div>
        );

      case 'restore':
        return (
          <div className="p-4 space-y-3">
            <div className="grid grid-cols-3 gap-1 p-1 rounded-xl bg-slate-100 dark:bg-[#0B192C] text-[11px] font-bold text-center">
              <div className="py-2 rounded-lg bg-white dark:bg-[#162A45]">{isAr ? 'الهاتف' : 'Phone'}</div>
              <div className="py-2 text-slate-500">SD Card</div>
              <div className="py-2 text-slate-500">USB OTG</div>
            </div>
            <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] text-xs space-y-1">
              <div className="font-mono-num font-bold">{currentBusiness.dbFileName}.enc</div>
              <div className="text-[11px] text-slate-500">{isAr ? 'مفتاح الاسترداد: DFTR-9F4A-...-B3A9' : 'Recovery Key: DFTR-9F4A-...-B3A9'}</div>
            </div>
            <button type="button" onClick={() => setRestoreChecked(true)} className="w-full min-h-[42px] rounded-xl border border-[#C89D42] text-[#9A7222] dark:text-[#D9B15B] text-xs font-bold">
              {isAr ? 'اختبار سلامة النسخة قبل الاستبدال' : 'Test Archive Integrity First'}
            </button>
            {restoreChecked && (
              <div className="p-3 rounded-xl border border-emerald-200 dark:border-emerald-900 bg-emerald-50/70 dark:bg-emerald-950/30 text-xs text-[#15803D] dark:text-[#22C55E] font-bold">
                {isAr ? 'سلامة SHA-256 مؤكدة · جاهز للاستعادة بموافقتك' : 'SHA-256 Verified · Ready for user confirmation'}
              </div>
            )}
            <button type="button" onClick={() => onNavigate('login')} className="w-full min-h-[44px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold">
              {isAr ? 'تأكيد الاستعادة' : 'Confirm Restore'}
            </button>
          </div>
        );

      case 'dashboard':
        return (
          <div className="p-4 space-y-3">
            <div className="bg-[#0B192C] text-white rounded-2xl p-4 space-y-2.5">
              <div className="flex items-center justify-between text-[11px] text-slate-300">
                <span>{currentBusiness.activityName[lang]}</span>
                <span className="font-mono-num text-[#D9B15B]">{cur} · {currentBusiness.fiscalStart.slice(0, 4)}</span>
              </div>
              <div>
                <div className="text-[11px] text-slate-300">{isAr ? 'إجمالي السيولة (نقدي + بنوك)' : 'Total Liquidity (Cash + Banks)'}</div>
                <div className="text-2xl font-bold font-mono-num mt-0.5">148,920.50</div>
              </div>
              <div className="grid grid-cols-3 gap-2 pt-2 border-t border-white/10 text-[11px]">
                <div><span className="text-slate-400 block">{isAr ? 'المبيعات' : 'Sales'}</span><span className="font-mono-num font-bold text-emerald-400">+6,450</span></div>
                <div><span className="text-slate-400 block">{isAr ? 'المصروفات' : 'Expenses'}</span><span className="font-mono-num font-bold text-red-400">-1,120</span></div>
                <div><span className="text-slate-400 block">{isAr ? 'الصافي' : 'Net'}</span><span className="font-mono-num font-bold text-[#D9B15B]">21,900</span></div>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button type="button" onClick={() => onNavigate('create-invoice')} className="min-h-[42px] px-3 rounded-xl bg-[#C89D42] text-[#0B192C] text-xs font-bold flex items-center justify-center gap-1.5">
                <FilePlus2 className="w-4 h-4" />
                <span>{isAr ? 'فاتورة جديدة' : 'New Invoice'}</span>
              </button>
              <button type="button" onClick={() => onNavigate('ai-assistant')} className="min-h-[42px] px-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#0F1E33] text-xs font-bold flex items-center justify-center gap-1.5">
                <Sparkles className="w-4 h-4 text-[#C89D42]" />
                <span>{isAr ? 'المساعد الذكي' : 'Assistant'}</span>
              </button>
            </div>
            <button type="button" onClick={() => onNavigate('notifications')} className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between text-start">
              <div className="flex items-center gap-2.5">
                <Bell className="w-4 h-4 text-[#C89D42]" />
                <div>
                  <div className="text-xs font-bold">{isAr ? 'تنبيهات النشاط المحلية' : 'Local Activity Alerts'}</div>
                  <div className="text-[10px] text-slate-500">{isAr ? '3 تنبيهات مخزون ونسخ احتياطي' : '3 stock & backup notices'}</div>
                </div>
              </div>
              <ForwardIcon className="w-4 h-4 text-slate-400" />
            </button>
            <button type="button" onClick={() => onNavigate('reports')} className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between text-start">
              <div>
                <div className="text-xs font-bold">{isAr ? 'سلامة القيود المزدوجة' : 'Double-Entry Health'}</div>
                <div className="text-[11px] text-[#15803D] dark:text-[#22C55E]">{isAr ? 'كافة القيود متوازنة · 0 أخطاء' : 'All journals balanced · 0 errors'}</div>
              </div>
              <ForwardIcon className="w-4 h-4 text-slate-400" />
            </button>
          </div>
        );

      case 'main-menu':
        return (
          <div className="p-4 space-y-3">
            <div className="p-3 rounded-xl bg-[#0B192C] text-white flex items-center justify-between text-xs">
              <div>
                <div className="text-[10px] text-slate-300">{currentBusiness.activityName[lang]}</div>
                <div className="font-bold text-[#D9B15B]">{currentBusiness.name[lang]}</div>
              </div>
              <span className="font-mono-num font-bold">{activeModules.length} / 37</span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              {activeModules.map((mod) => (
                <button
                  key={mod.id}
                  type="button"
                  onClick={() => onNavigate(mod.targetScreen)}
                  className="p-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] text-start hover:border-[#C89D42] transition-colors"
                >
                  <div className="font-mono-num text-[10px] font-bold text-[#9A7222] dark:text-[#D9B15B]">
                    {String(mod.number).padStart(2, '0')}
                  </div>
                  <div className="text-[11px] font-bold mt-0.5 leading-snug">{mod.name[lang]}</div>
                </button>
              ))}
            </div>
          </div>
        );

      case 'accounts':
        return (
          <div className="p-4 space-y-2.5">
            <div className="text-[11px] text-slate-500">{isAr ? 'دليل الحسابات (الأرصدة مشتقة من القيود فقط):' : 'Chart of Accounts (Balances derived strictly from entries):'}</div>
            {[
              { code: '1000', name: isAr ? 'الأصول' : 'Assets', bal: '342,500.00' },
              { code: '2000', name: isAr ? 'الالتزامات' : 'Liabilities', bal: '84,200.00' },
              { code: '3000', name: isAr ? 'حقوق الملكية' : 'Equity', bal: '200,000.00' },
              { code: '4000', name: isAr ? 'الإيرادات' : 'Revenue', bal: '124,600.00' },
              { code: '5000', name: isAr ? 'المصروفات' : 'Expenses', bal: '66,300.00' },
            ].map((acc) => (
              <button
                key={acc.code}
                type="button"
                onClick={() => onNavigate('general-ledger')}
                className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between text-start"
              >
                <div className="flex items-center gap-2">
                  <span className="font-mono-num text-[11px] font-bold text-[#9A7222] dark:text-[#D9B15B]">{acc.code}</span>
                  <span className="text-xs font-bold">{acc.name}</span>
                </div>
                <span className="font-mono-num text-xs font-bold">{acc.bal} {cur}</span>
              </button>
            ))}
            <div className="grid grid-cols-2 gap-2 pt-1">
              <button type="button" onClick={() => onNavigate('journal-entries')} className="min-h-[40px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold">
                {isAr ? 'القيود اليومية' : 'Journals'}
              </button>
              <button type="button" onClick={() => onNavigate('treasury-banks')} className="min-h-[40px] rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-bold">
                {isAr ? 'الخزائن والبنوك' : 'Treasury'}
              </button>
            </div>
          </div>
        );

      case 'sales':
        return (
          <div className="p-4 space-y-3">
            <div className="grid grid-cols-3 gap-1 p-1 rounded-xl bg-slate-100 dark:bg-[#0B192C]">
              {([['all', isAr ? 'الكل' : 'All'], ['paid', isAr ? 'مدفوعة' : 'Paid'], ['credit', isAr ? 'آجلة' : 'Credit']] as const).map(([k, l]) => (
                <button
                  key={k}
                  type="button"
                  onClick={() => setSelectedSalesFilter(k)}
                  className={`py-1.5 rounded-lg text-[11px] font-bold ${
                    selectedSalesFilter === k ? 'bg-[#0B192C] text-white dark:bg-[#C89D42] dark:text-[#0B192C]' : 'text-slate-500'
                  }`}
                >
                  {l}
                </button>
              ))}
            </div>
            {[
              { id: 'INV-1042', p: isAr ? 'شركة المدار' : 'Al-Madar Co.', amt: '4,600.00', type: 'credit', jv: 'JV-884' },
              { id: 'INV-1041', p: isAr ? 'عميل نقدي' : 'Cash Client', amt: '920.00', type: 'paid', jv: 'JV-883' },
            ]
              .filter((x) => selectedSalesFilter === 'all' || x.type === selectedSalesFilter)
              .map((inv) => (
                <div key={inv.id} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex justify-between items-center">
                  <div>
                    <div className="text-xs font-bold">{inv.p}</div>
                    <div className="font-mono-num text-[10px] text-slate-500">{inv.id} · {inv.jv}</div>
                  </div>
                  <span className="font-mono-num text-xs font-bold text-[#15803D] dark:text-[#22C55E]">{inv.amt} {cur}</span>
                </div>
              ))}
            <div className="grid grid-cols-2 gap-2">
              <button type="button" onClick={() => onNavigate('create-invoice')} className="min-h-[42px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold">
                {isAr ? '+ فاتورة بيع' : '+ New Invoice'}
              </button>
              <button type="button" onClick={() => onNavigate('pos-terminal')} className="min-h-[42px] rounded-xl border border-[#C89D42] text-[#9A7222] dark:text-[#D9B15B] text-xs font-bold">
                {isAr ? 'نقطة POS' : 'POS Terminal'}
              </button>
            </div>
          </div>
        );

      case 'create-invoice':
        return (
          <div className="p-4 space-y-3">
            <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between">
              <div>
                <div className="text-xs font-bold">{isAr ? 'صنف تجاري قياسي' : 'Standard Stock Unit'}</div>
                <div className="font-mono-num text-[11px] text-slate-500">500.00 × {invoiceQty} {cur}</div>
              </div>
              <div className="flex items-center gap-2">
                <button type="button" onClick={() => { setInvoicePosted(false); setInvoiceQty((q) => Math.max(1, q - 1)); }} className="w-8 h-8 rounded-lg border border-slate-200 dark:border-slate-700 flex items-center justify-center"><Minus className="w-3.5 h-3.5" /></button>
                <span className="font-mono-num text-xs font-bold">{invoiceQty}</span>
                <button type="button" onClick={() => { setInvoicePosted(false); setInvoiceQty((q) => q + 1); }} className="w-8 h-8 rounded-lg border border-slate-200 dark:border-slate-700 flex items-center justify-center"><Plus className="w-3.5 h-3.5" /></button>
              </div>
            </div>
            <div className="p-3 rounded-xl bg-[#0B192C] text-white space-y-1.5 text-[11px]">
              <div className="flex justify-between text-[#D9B15B] font-bold">
                <span>{isAr ? 'معاينة القيد المزدوج' : 'Double-Entry Preview'}</span>
                <span>{isAr ? 'متوازن' : 'Balanced'}</span>
              </div>
              <div className="flex justify-between font-mono-num"><span>{isAr ? 'مدين: العملاء' : 'DR: Receivables'}</span><span>{totalAmount.toFixed(2)}</span></div>
              <div className="flex justify-between font-mono-num"><span>{isAr ? 'دائن: المبيعات' : 'CR: Revenue'}</span><span>{subtotal.toFixed(2)}</span></div>
              <div className="flex justify-between font-mono-num"><span>{isAr ? 'دائن: ضريبة 15%' : 'CR: VAT 15%'}</span><span>{vatAmount.toFixed(2)}</span></div>
            </div>
            {invoicePosted && (
              <div className="p-2.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-900 text-[#15803D] dark:text-[#22C55E] text-xs font-bold">
                {isAr ? 'تم اعتماد الفاتورة وترحيل القيد (محاكاة تصميمية)' : 'Invoice posted atomically (Design Preview)'}
              </div>
            )}
            <button type="button" onClick={() => setInvoicePosted(true)} className="w-full min-h-[44px] rounded-xl bg-[#C89D42] text-[#0B192C] text-xs font-bold">
              {isAr ? 'اعتماد الفاتورة وترحيل القيد' : 'Approve & Post Atomic Entry'}
            </button>
          </div>
        );

      case 'purchases':
        return (
          <div className="p-4 space-y-3">
            <div className="p-3 rounded-xl bg-white dark:bg-[#0F1E33] border border-slate-200 dark:border-slate-800 flex justify-between text-xs">
              <span>{isAr ? 'تكلفة المخزون:' : 'Costing:'}</span>
              <span className="font-bold text-[#9A7222] dark:text-[#D9B15B]">{isAr ? 'المتوسط المرجح / FIFO' : 'Weighted Avg / FIFO'}</span>
            </div>
            {[
              { id: 'PUR-409', sup: isAr ? 'مصنع التوريدات المتحدة' : 'United Supply Co.', total: '18,500.00' },
              { id: 'PUR-408', sup: isAr ? 'مؤسسة الإمداد' : 'Al-Imdad Est.', total: '6,240.00' },
            ].map((b) => (
              <div key={b.id} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex justify-between items-center">
                <div><div className="text-xs font-bold">{b.sup}</div><div className="font-mono-num text-[10px] text-slate-500">{b.id}</div></div>
                <span className="font-mono-num text-xs font-bold text-[#DC2626] dark:text-[#F87171]">{b.total} {cur}</span>
              </div>
            ))}
          </div>
        );

      case 'parties':
        return (
          <div className="p-4 space-y-3">
            <div className="grid grid-cols-2 gap-1 p-1 rounded-xl bg-slate-100 dark:bg-[#0B192C]">
              {([['customers', isAr ? 'العملاء' : 'Customers'], ['suppliers', isAr ? 'الموردون' : 'Suppliers']] as const).map(([k, l]) => (
                <button key={k} type="button" onClick={() => setSelectedPartyTab(k)} className={`py-2 rounded-lg text-[11px] font-bold ${selectedPartyTab === k ? 'bg-[#0B192C] text-white dark:bg-[#C89D42] dark:text-[#0B192C]' : 'text-slate-500'}`}>{l}</button>
              ))}
            </div>
            {(selectedPartyTab === 'customers'
              ? [{ n: isAr ? 'شركة المدار' : 'Al-Madar Co.', b: '18,400.00' }]
              : [{ n: isAr ? 'مصنع التوريدات' : 'United Supply', b: '9,200.00' }]
            ).map((p) => (
              <div key={p.n} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex justify-between text-xs">
                <span className="font-bold">{p.n}</span>
                <span className="font-mono-num font-bold">{p.b} {cur}</span>
              </div>
            ))}
          </div>
        );

      case 'inventory':
        return (
          <div className="p-4 space-y-2.5">
            <div className="p-3 rounded-xl bg-[#0B192C] text-white flex justify-between text-xs">
              <span>{isAr ? 'المستودع الرئيسي' : 'Main Warehouse'}</span>
              <span className="font-mono-num text-[#D9B15B]">142 SKUs</span>
            </div>
            {[
              { name: isAr ? 'صنف قياسي #101' : 'Standard SKU #101', qty: '64', low: false },
              { name: isAr ? 'صنف حرج #209 (تحت حد الطلب)' : 'Critical SKU #209 (Low)', qty: '4', low: true },
            ].map((i) => (
              <div key={i.name} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex justify-between text-xs">
                <span className={`font-bold ${i.low ? 'text-[#DC2626] dark:text-[#F87171]' : ''}`}>{i.name}</span>
                <span className="font-mono-num font-bold">{i.qty}</span>
              </div>
            ))}
            <button type="button" onClick={() => onNavigate('barcode-qr')} className="w-full min-h-[40px] rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-bold flex items-center justify-center gap-1.5">
              <QrCode className="w-3.5 h-3.5 text-[#C89D42]" />
              <span>{isAr ? 'إدارة الباركود وQR' : 'Barcode & QR Labels'}</span>
            </button>
          </div>
        );

      case 'employees':
        return (
          <div className="p-4 space-y-2.5">
            {[
              { n: isAr ? 'أحمد السالم' : 'Ahmed Al-Salem', r: isAr ? 'محاسب عام' : 'Accountant', s: '6,500' },
              { n: isAr ? 'سارة العتيبي' : 'Sara Al-Otaibi', r: isAr ? 'أمينة صندوق' : 'Cashier', s: '4,800' },
            ].map((e) => (
              <div key={e.n} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex justify-between text-xs">
                <div><div className="font-bold">{e.n}</div><div className="text-[10px] text-slate-500">{e.r}</div></div>
                <span className="font-mono-num font-bold">{e.s} {cur}</span>
              </div>
            ))}
            <button type="button" onClick={() => onNavigate('journal-entries')} className="w-full min-h-[42px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold">
              {isAr ? 'ترحيل قيد الرواتب الشهري' : 'Post Payroll Journal'}
            </button>
          </div>
        );

      default:
        return renderExtraMobileScreen({
          screen: activeScreen,
          lang,
          business: currentBusiness,
          onNavigate,
          statementTab,
          setStatementTab,
          posCartCount,
          setPosCartCount,
          auditRan,
          setAuditRan,
          aiQueryIndex,
          setAiQueryIndex,
          backupVerified,
          setBackupVerified,
          showRecoveryKey,
          setShowRecoveryKey,
          onToggleLang,
          onToggleTheme,
          theme,
        });
    }
  };

  // Wrapper handling all 5 UI states cleanly (populated, loading, empty, success, error)
  const renderStatefulBody = () => {
    // Keep splash and login usable regardless, unless explicitly testing loading/error/empty
    if (uiState === 'loading') {
      return (
        <div className="p-4 space-y-3 animate-pulse">
          <div className="h-24 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          <div className="grid grid-cols-2 gap-2">
            <div className="h-11 rounded-xl bg-slate-200 dark:bg-slate-800" />
            <div className="h-11 rounded-xl bg-slate-200 dark:bg-slate-800" />
          </div>
          <div className="h-16 rounded-xl bg-slate-200 dark:bg-slate-800" />
          <div className="h-16 rounded-xl bg-slate-200 dark:bg-slate-800" />
          <div className="text-center text-[11px] text-slate-400 pt-2">
            {isAr ? 'جاري التحقق من قاعدة SQLCipher المحلية...' : 'Loading local SQLCipher records...'}
          </div>
        </div>
      );
    }

    if (uiState === 'empty') {
      return (
        <div className="p-6 h-full flex flex-col items-center justify-center text-center space-y-3">
          <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-[#0F1E33] border border-slate-200 dark:border-slate-800 flex items-center justify-center text-[#C89D42]">
            <FolderOpen className="w-6 h-6" />
          </div>
          <div className="text-sm font-bold text-[#0B192C] dark:text-slate-100">
            {isAr ? 'لا توجد سجلات في هذه الشاشة بعد' : 'No Records Available Yet'}
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
            {isAr
              ? `هذه الحالة توضح مظهر "${currentScreenSpec.title.ar}" عند عدم وجود بيانات مسجلة للنشاط.`
              : `Empty state preview for "${currentScreenSpec.title.en}" when no activity records exist yet.`}
          </p>
          <button
            type="button"
            onClick={() => onChangeUiState('populated')}
            className="min-h-[42px] px-5 rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold"
          >
            {isAr ? 'إضافة أول عملية / معاينة المحتوى' : 'Log First Entry / View Layout'}
          </button>
        </div>
      );
    }

    return (
      <div>
        {uiState === 'success' && (
          <div className="mx-4 mt-3 p-3 rounded-xl border border-emerald-200 dark:border-emerald-900 bg-emerald-50/80 dark:bg-emerald-950/30 flex items-start gap-2 text-xs text-[#15803D] dark:text-[#22C55E]">
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
            <div>
              <div className="font-bold">{isAr ? 'تم تنفيذ العملية بتوازن كامل' : 'Transaction Verified & Balanced'}</div>
              <div className="text-[11px] opacity-90">{isAr ? 'تم الترحيل الذري وحفظ سجل التدقيق محلياً.' : 'Atomic Room commit & local audit log recorded.'}</div>
            </div>
          </div>
        )}
        {uiState === 'error' && (
          <div className="mx-4 mt-3 p-3 rounded-xl border border-red-200 dark:border-red-900 bg-red-50/80 dark:bg-red-950/30 flex items-start gap-2 text-xs text-[#DC2626] dark:text-[#F87171]">
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
            <div>
              <div className="font-bold">{isAr ? 'تعذّر الحفظ — قيد غير متوازن أو صلاحية مقيدة' : 'Blocked — Unbalanced Entry or Restricted Role'}</div>
              <div className="text-[11px] opacity-90">{isAr ? 'الرجاء مطابقة إجمالي المدين مع الدائن قبل الاعتماد.' : 'Ensure Total Debit equals Total Credit before posting.'}</div>
            </div>
          </div>
        )}
        {uiState === 'populated' && activeScreen !== 'splash' && (
          <div className="mx-4 mt-3 px-2.5 py-1 rounded-lg bg-slate-100 dark:bg-[#0F1E33] text-[10px] text-slate-500 dark:text-slate-400 flex items-center justify-between">
            <span>{isAr ? 'بيانات تصميمية للمعاينة فقط' : 'Design Preview Data Only'}</span>
            <span className="font-mono-num">{currentBusiness.dbFileName}</span>
          </div>
        )}
        {renderPrimaryScreens()}
      </div>
    );
  };

  return (
    <div className="mx-auto w-full max-w-[365px]">
      <div className="rounded-[38px] p-2.5 bg-[#0B192C] border-[3px] border-slate-700 shadow-2xl">
        <div
          className={`rounded-[30px] overflow-hidden flex flex-col h-[650px] ${
            theme === 'dark' ? 'bg-[#08111E] text-slate-100' : 'bg-[#F8FAFC] text-[#0B192C]'
          }`}
        >
          {/* Android Status Bar */}
          <div className="px-5 pt-2.5 pb-1.5 flex items-center justify-between text-[10px] font-mono-num opacity-75 shrink-0">
            <span>09:41</span>
            <div className="w-16 h-3 rounded-full bg-black/30 dark:bg-white/15" />
            <span>{isAr ? 'دون إنترنت' : 'OFFLINE'}</span>
          </div>

          {/* Mobile App Top Bar */}
          {activeScreen !== 'splash' && (
            <div
              className={`px-3 h-13 border-b flex items-center justify-between gap-2 shrink-0 ${
                theme === 'dark' ? 'bg-[#0F1E33] border-slate-800' : 'bg-white border-slate-200'
              }`}
            >
              <button
                type="button"
                onClick={onBack}
                className="w-9 h-9 rounded-lg flex items-center justify-center hover:bg-slate-100 dark:hover:bg-slate-800 shrink-0"
                title={isAr ? 'رجوع' : 'Back'}
              >
                <BackIcon className="w-4 h-4" />
              </button>
              <div className="min-w-0 text-center">
                <div className="text-xs font-bold truncate">{currentScreenSpec.title[lang]}</div>
                <div className="text-[10px] text-slate-500 dark:text-slate-400 truncate">
                  {currentBusiness.activityName[lang]}
                </div>
              </div>
              <button
                type="button"
                onClick={() => onNavigate('main-menu')}
                className="w-9 h-9 rounded-lg flex items-center justify-center hover:bg-slate-100 dark:hover:bg-slate-800 shrink-0"
                title={isAr ? 'القائمة الرئيسية' : 'Main Menu'}
              >
                <LayoutGrid className="w-4 h-4 text-[#C89D42]" />
              </button>
            </div>
          )}

          {/* Scrollable Body */}
          <div className="flex-1 overflow-y-auto daftar-scroll">{renderStatefulBody()}</div>

          {/* Thumb-Zone Bottom Bar */}
          {showBottomBar && (
            <div
              className={`h-16 px-2 border-t grid grid-cols-5 items-center shrink-0 ${
                theme === 'dark' ? 'bg-[#0F1E33] border-slate-800' : 'bg-white border-slate-200'
              }`}
            >
              {(
                [
                  { id: 'dashboard', icon: Home, label: isAr ? 'الرئيسية' : 'Home' },
                  { id: 'sales', icon: Receipt, label: isAr ? 'المبيعات' : 'Sales' },
                  { id: 'accounts', icon: BookOpen, label: isAr ? 'الحسابات' : 'Ledger' },
                  { id: 'reports', icon: BarChart3, label: isAr ? 'التقارير' : 'Reports' },
                  { id: 'main-menu', icon: LayoutGrid, label: isAr ? 'الوحدات' : 'Modules' },
                ] as const
              ).map((tab) => {
                const Icon = tab.icon;
                const active = activeScreen === tab.id;
                return (
                  <button
                    key={tab.id}
                    type="button"
                    onClick={() => onNavigate(tab.id)}
                    className={`flex flex-col items-center justify-center min-h-[44px] gap-1 ${
                      active ? 'text-[#C89D42] font-bold' : 'text-slate-500 dark:text-slate-400'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                    <span className="text-[10px] whitespace-nowrap">{tab.label}</span>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
