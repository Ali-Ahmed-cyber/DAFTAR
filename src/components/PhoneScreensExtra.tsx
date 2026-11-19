import React from 'react';
import {
  AlertTriangle,
  BarChart3,
  Bell,
  Building2,
  CheckCircle2,
  CreditCard,
  Factory,
  FileText,
  FolderKanban,
  Gift,
  Layers,
  MapPin,
  Package,
  Paperclip,
  Pill,
  QrCode,
  Receipt,
  ShieldCheck,
  Sparkles,
  Truck,
  UtensilsCrossed,
  Wallet,
  Wrench,
} from 'lucide-react';
import { BusinessTypeSpec, Language, ScreenId } from '../types/daftar';

interface ExtraScreenProps {
  screen: ScreenId;
  lang: Language;
  business: BusinessTypeSpec;
  onNavigate: (s: ScreenId) => void;
  statementTab: 'pnl' | 'bs';
  setStatementTab: (t: 'pnl' | 'bs') => void;
  posCartCount: number;
  setPosCartCount: React.Dispatch<React.SetStateAction<number>>;
  auditRan: boolean;
  setAuditRan: (v: boolean) => void;
  aiQueryIndex: number;
  setAiQueryIndex: (i: number) => void;
  backupVerified: boolean;
  setBackupVerified: (v: boolean) => void;
  showRecoveryKey: boolean;
  setShowRecoveryKey: React.Dispatch<React.SetStateAction<boolean>>;
  onToggleLang: () => void;
  onToggleTheme: () => void;
  theme: 'light' | 'dark';
}

export const renderExtraMobileScreen = (props: ExtraScreenProps): React.ReactNode => {
  const {
    screen,
    lang,
    business,
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
  } = props;

  const isAr = lang === 'ar';
  const cur = business.defaultCurrency;

  switch (screen) {
    case 'notifications':
      return (
        <div className="p-4 space-y-2.5">
          {[
            {
              title: isAr ? 'تنبيه مخزون: صنف #209 تحت حد الطلب' : 'Low Stock: SKU #209 below min',
              cat: isAr ? 'المخزون · WorkManager' : 'Inventory · WorkManager',
              target: 'inventory' as ScreenId,
              tone: 'text-[#B45309] dark:text-[#FBBF24]',
            },
            {
              title: isAr ? 'نسخ احتياطي مجدول جاهز للتحقق' : 'Scheduled backup ready to verify',
              cat: isAr ? 'النسخ المشفر · قبل ساعتين' : 'Encrypted Vault · 2h ago',
              target: 'backup' as ScreenId,
              tone: 'text-[#15803D] dark:text-[#22C55E]',
            },
            {
              title: isAr ? 'فاتورة عميل مستحقة التحصيل (#1038)' : 'Overdue Customer Invoice (#1038)',
              cat: isAr ? 'الذمم · متأخرة 3 أيام' : 'Receivables · 3d overdue',
              target: 'parties' as ScreenId,
              tone: 'text-[#DC2626] dark:text-[#F87171]',
            },
          ].map((n) => (
            <button
              key={n.title}
              type="button"
              onClick={() => onNavigate(n.target)}
              className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] text-start space-y-1"
            >
              <div className={`text-[10px] font-bold ${n.tone}`}>{n.cat}</div>
              <div className="text-xs font-bold text-[#0B192C] dark:text-slate-100">{n.title}</div>
            </button>
          ))}
        </div>
      );

    case 'journal-entries':
      return (
        <div className="p-4 space-y-3">
          <div className="p-3 rounded-xl bg-[#0B192C] text-white text-xs space-y-1.5">
            <div className="flex justify-between font-bold text-[#D9B15B]">
              <span>{isAr ? 'قاعدة التوازن الإلزامية' : 'Strict Double-Entry Rule'}</span>
              <span className="font-mono-num">Σ DR = Σ CR</span>
            </div>
            <p className="text-[11px] text-slate-300">
              {isAr
                ? 'يُمنع حفظ أي قيد غير متوازن أو حذف قيد معتمد؛ التصحيح يتم بقيد عكسي.'
                : 'Unbalanced entries and direct deletions are blocked; corrections use reversal entries.'}
            </p>
          </div>
          {[
            { id: 'JV-884', desc: isAr ? 'ترحيل فاتورة مبيعات INV-1042' : 'Auto Sale INV-1042', amt: '4,600.00', status: isAr ? 'متوازن' : 'Balanced' },
            { id: 'JV-883', desc: isAr ? 'سداد مورد — فاتورة PUR-409' : 'Supplier Payment PUR-409', amt: '1,850.00', status: isAr ? 'متوازن' : 'Balanced' },
          ].map((j) => (
            <div key={j.id} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-1.5">
              <div className="flex justify-between text-xs font-bold">
                <span className="font-mono-num text-[#9A7222] dark:text-[#D9B15B]">{j.id}</span>
                <span className="text-[#15803D] dark:text-[#22C55E]">{j.status}</span>
              </div>
              <div className="text-xs">{j.desc}</div>
              <div className="flex justify-between text-[11px] font-mono-num text-slate-500 pt-1 border-t border-slate-100 dark:border-slate-800">
                <span>{isAr ? 'مدين:' : 'DR:'} {j.amt} {cur}</span>
                <span>{isAr ? 'دائن:' : 'CR:'} {j.amt} {cur}</span>
              </div>
            </div>
          ))}
          <button
            type="button"
            onClick={() => onNavigate('general-ledger')}
            className="w-full min-h-[42px] rounded-xl border border-[#C89D42] text-[#9A7222] dark:text-[#D9B15B] text-xs font-bold"
          >
            {isAr ? 'عرض دفتر الأستاذ العام' : 'Open General Ledger'}
          </button>
        </div>
      );

    case 'general-ledger':
      return (
        <div className="p-4 space-y-3">
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between text-xs">
            <span className="font-bold">{isAr ? 'حساب: 1101 الخزينة الرئيسية' : 'Acct: 1101 Main Cash'}</span>
            <span className="font-mono-num font-bold text-[#15803D] dark:text-[#22C55E]">38,420.50 {cur}</span>
          </div>
          <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] divide-y divide-slate-100 dark:divide-slate-800 text-[11px]">
            {[
              { d: '11-18', m: isAr ? 'تحصيل فاتورة INV-1041' : 'Cash Sale INV-1041', dr: '920.00', cr: '—', bal: '38,420.50' },
              { d: '11-17', m: isAr ? 'مصروف كهرباء' : 'Utility Expense', dr: '—', cr: '450.00', bal: '37,500.50' },
              { d: '11-15', m: isAr ? 'رصيد منقول' : 'Opening Balance', dr: '37,950.50', cr: '—', bal: '37,950.50' },
            ].map((row, idx) => (
              <div key={idx} className="p-2.5 flex items-center justify-between gap-2">
                <div>
                  <div className="font-bold">{row.m}</div>
                  <div className="font-mono-num text-[10px] text-slate-500">{row.d} · DR {row.dr} · CR {row.cr}</div>
                </div>
                <div className="font-mono-num font-bold">{row.bal}</div>
              </div>
            ))}
          </div>
        </div>
      );

    case 'treasury-banks':
      return (
        <div className="p-4 space-y-3">
          {[
            { name: isAr ? 'الخزينة الرئيسية (نقدي)' : 'Main Cash Vault', bal: '38,420.50' },
            { name: isAr ? 'حساب البنك الأهلي الجاري' : 'National Bank Current', bal: '110,500.00' },
          ].map((v) => (
            <div key={v.name} className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <Wallet className="w-4 h-4 text-[#C89D42]" />
                <span className="text-xs font-bold">{v.name}</span>
              </div>
              <span className="font-mono-num text-xs font-bold text-[#15803D] dark:text-[#22C55E]">{v.bal} {cur}</span>
            </div>
          ))}
          <button
            type="button"
            onClick={() => onNavigate('journal-entries')}
            className="w-full min-h-[42px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold"
          >
            {isAr ? 'تحويل بين خزينة وبنك (قيد آلي)' : 'Transfer Between Vaults (Auto Journal)'}
          </button>
        </div>
      );

    case 'expenses-revenues':
      return (
        <div className="p-4 space-y-2.5">
          {[
            { type: isAr ? 'مصروف' : 'Expense', name: isAr ? 'إيجار المقر الشهري' : 'Monthly Rent', amt: '-4,500.00', color: 'text-[#DC2626] dark:text-[#F87171]' },
            { type: isAr ? 'إيراد متنوع' : 'Other Income', name: isAr ? 'رسوم توصيل إضافية' : 'Delivery Surcharge', amt: '+680.00', color: 'text-[#15803D] dark:text-[#22C55E]' },
          ].map((item) => (
            <div key={item.name} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between">
              <div>
                <div className="text-[10px] text-slate-500">{item.type}</div>
                <div className="text-xs font-bold">{item.name}</div>
              </div>
              <span className={`font-mono-num text-xs font-bold ${item.color}`}>{item.amt} {cur}</span>
            </div>
          ))}
          <button
            type="button"
            onClick={() => onNavigate('attachments')}
            className="w-full min-h-[42px] rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-bold flex items-center justify-center gap-1.5"
          >
            <Paperclip className="w-3.5 h-3.5 text-[#C89D42]" />
            <span>{isAr ? 'مرفقات سندات الصرف والقبض' : 'Voucher Receipt Attachments'}</span>
          </button>
        </div>
      );

    case 'fixed-assets':
      return (
        <div className="p-4 space-y-3">
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-1.5 text-xs">
            <div className="flex justify-between font-bold">
              <span>{isAr ? 'أجهزة نقاط بيع وموازين' : 'POS Terminals & Scales'}</span>
              <span className="font-mono-num">18,000 {cur}</span>
            </div>
            <div className="text-[11px] text-slate-500">
              {isAr ? 'القسط الثابت 20% سنوياً · مجمع الإهلاك: 3,600' : 'Straight-line 20% p.a. · Accum. Dep: 3,600'}
            </div>
            <div className="text-[11px] font-mono-num text-[#15803D] dark:text-[#22C55E]">
              {isAr ? 'صافي القيمة الدفترية: 14,400.00' : 'Net Book Value: 14,400.00'}
            </div>
          </div>
          <button
            type="button"
            onClick={() => onNavigate('journal-entries')}
            className="w-full min-h-[42px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold"
          >
            {isAr ? 'ترحيل قيد الإهلاك الشهري' : 'Post Monthly Depreciation Journal'}
          </button>
        </div>
      );

    case 'trial-balance':
      return (
        <div className="p-4 space-y-3">
          <div className="p-3 rounded-xl bg-[#0B192C] text-white flex items-center justify-between text-xs">
            <span>{isAr ? 'توازن ميزان المراجعة' : 'Trial Balance Parity'}</span>
            <span className="font-mono-num text-emerald-400 font-bold">490,800 = 490,800</span>
          </div>
          <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] divide-y divide-slate-100 dark:divide-slate-800 text-[11px]">
            {[
              { acct: isAr ? 'الأصول المتداولة' : 'Current Assets', dr: '215,400', cr: '—' },
              { acct: isAr ? 'الموردون والضرائب' : 'Payables & VAT', dr: '—', cr: '84,200' },
              { acct: isAr ? 'رأس المال' : 'Capital Equity', dr: '—', cr: '200,000' },
              { acct: isAr ? 'المبيعات والمشتريات' : 'Sales & COGS', dr: '275,400', cr: '206,600' },
            ].map((r) => (
              <div key={r.acct} className="p-2.5 flex justify-between font-mono-num">
                <span className="font-sans font-bold">{r.acct}</span>
                <span>{r.dr} / {r.cr}</span>
              </div>
            ))}
          </div>
        </div>
      );

    case 'financial-statements':
      return (
        <div className="p-4 space-y-3">
          <div className="grid grid-cols-2 gap-1 p-1 rounded-xl bg-slate-100 dark:bg-[#0B192C]">
            <button
              type="button"
              onClick={() => setStatementTab('pnl')}
              className={`py-2 rounded-lg text-[11px] font-bold ${
                statementTab === 'pnl' ? 'bg-[#0B192C] text-white dark:bg-[#C89D42] dark:text-[#0B192C]' : 'text-slate-500'
              }`}
            >
              {isAr ? 'الأرباح والخسائر' : 'Profit & Loss'}
            </button>
            <button
              type="button"
              onClick={() => setStatementTab('bs')}
              className={`py-2 rounded-lg text-[11px] font-bold ${
                statementTab === 'bs' ? 'bg-[#0B192C] text-white dark:bg-[#C89D42] dark:text-[#0B192C]' : 'text-slate-500'
              }`}
            >
              {isAr ? 'الميزانية العمومية' : 'Balance Sheet'}
            </button>
          </div>
          {statementTab === 'pnl' ? (
            <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-2 text-xs">
              <div className="flex justify-between"><span>{isAr ? 'إجمالي الإيرادات' : 'Total Revenue'}</span><span className="font-mono-num">124,600.00</span></div>
              <div className="flex justify-between"><span>{isAr ? 'التكاليف والمصروفات' : 'Costs & Expenses'}</span><span className="font-mono-num text-[#DC2626] dark:text-[#F87171]">-66,300.00</span></div>
              <div className="flex justify-between pt-2 border-t border-slate-100 dark:border-slate-800 font-bold text-[#15803D] dark:text-[#22C55E]">
                <span>{isAr ? 'صافي الربح' : 'Net Profit'}</span>
                <span className="font-mono-num">+58,300.00 {cur}</span>
              </div>
            </div>
          ) : (
            <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-2 text-xs">
              <div className="flex justify-between font-bold"><span>{isAr ? 'إجمالي الأصول' : 'Total Assets'}</span><span className="font-mono-num">342,500.00</span></div>
              <div className="flex justify-between"><span>{isAr ? 'إجمالي الالتزامات' : 'Total Liabilities'}</span><span className="font-mono-num">84,200.00</span></div>
              <div className="flex justify-between"><span>{isAr ? 'حقوق الملكية والأرباح' : 'Equity + Retained'}</span><span className="font-mono-num">258,300.00</span></div>
              <div className="pt-2 border-t border-slate-100 dark:border-slate-800 text-[11px] font-bold text-[#15803D] dark:text-[#22C55E]">
                {isAr ? 'الأصول = الالتزامات + الحقوق (342,500 متطابقة)' : 'Assets = Liabilities + Equity (342,500 Balanced)'}
              </div>
            </div>
          )}
        </div>
      );

    case 'cash-flows':
      return (
        <div className="p-4 space-y-2.5">
          {[
            { label: isAr ? 'التدفق التشغيلي' : 'Operating Cash Flow', val: '+34,200.00', pos: true },
            { label: isAr ? 'التدفق الاستثماري (أصول)' : 'Investing Cash Flow', val: '-8,500.00', pos: false },
            { label: isAr ? 'التدفق التمويلي' : 'Financing Cash Flow', val: '0.00', pos: true },
          ].map((c) => (
            <div key={c.label} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex justify-between text-xs">
              <span className="font-bold">{c.label}</span>
              <span className={`font-mono-num font-bold ${c.pos ? 'text-[#15803D] dark:text-[#22C55E]' : 'text-[#DC2626] dark:text-[#F87171]'}`}>
                {c.val} {cur}
              </span>
            </div>
          ))}
        </div>
      );

    case 'pos-terminal':
      return (
        <div className="p-4 space-y-3">
          <div className="p-3 rounded-xl bg-[#0B192C] text-white flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <QrCode className="w-4 h-4 text-[#D9B15B]" />
              <span>{isAr ? 'نقطة بيع سريعة' : 'Touch POS'}</span>
            </div>
            <span className="font-mono-num text-[#D9B15B]">{posCartCount} {isAr ? 'أصناف' : 'Items'}</span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            {[
              { name: isAr ? 'صنف سريع 1' : 'Quick Item 1', p: 45 },
              { name: isAr ? 'صنف سريع 2' : 'Quick Item 2', p: 120 },
              { name: isAr ? 'صنف سريع 3' : 'Quick Item 3', p: 80 },
              { name: isAr ? 'صنف سريع 4' : 'Quick Item 4', p: 25 },
            ].map((item) => (
              <button
                key={item.name}
                type="button"
                onClick={() => setPosCartCount((c) => c + 1)}
                className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] text-start active:scale-95 transition-transform"
              >
                <div className="text-xs font-bold">{item.name}</div>
                <div className="font-mono-num text-xs text-[#15803D] dark:text-[#22C55E] mt-1">{item.p}.00 {cur}</div>
              </button>
            ))}
          </div>
          <button
            type="button"
            onClick={() => onNavigate('create-invoice')}
            className="w-full min-h-[44px] rounded-xl bg-[#C89D42] text-[#0B192C] text-xs font-bold"
          >
            {isAr ? `تحصيل وإصدار إيصال (${posCartCount})` : `Checkout & Post (${posCartCount})`}
          </button>
        </div>
      );

    case 'barcode-qr':
      return (
        <div className="p-4 space-y-3">
          <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] text-center space-y-2">
            <QrCode className="w-12 h-12 mx-auto text-[#0B192C] dark:text-[#D9B15B]" />
            <div className="font-mono-num text-xs font-bold">SKU-628100421009</div>
            <div className="text-[11px] text-slate-500">{isAr ? 'ملصق تسعير ورمز QR ضريبي محلي' : 'Local SKU Label & Tax QR'}</div>
          </div>
          <button
            type="button"
            onClick={() => onNavigate('pos-terminal')}
            className="w-full min-h-[42px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold"
          >
            {isAr ? 'فتح ماسح الكاميرا في POS' : 'Launch Scanner in POS'}
          </button>
        </div>
      );

    case 'budgets':
      return (
        <div className="p-4 space-y-3">
          {[
            { name: isAr ? 'موازنة التسويق' : 'Marketing Budget', used: 62, limit: '10,000' },
            { name: isAr ? 'موازنة التشغيل والصيانة' : 'Ops & Maintenance', used: 91, limit: '15,000' },
          ].map((b) => (
            <div key={b.name} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-2 text-xs">
              <div className="flex justify-between font-bold">
                <span>{b.name}</span>
                <span className="font-mono-num">{b.used}% / {b.limit}</span>
              </div>
              <div className="h-2 rounded-full bg-slate-100 dark:bg-[#0B192C] overflow-hidden">
                <div
                  className={`h-full rounded-full ${b.used > 85 ? 'bg-[#DC2626]' : 'bg-[#C89D42]'}`}
                  style={{ width: `${b.used}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      );

    case 'attachments':
      return (
        <div className="p-4 space-y-2.5">
          {[
            { file: 'inv_supplier_409.pdf', ref: 'PUR-409', size: '420 KB' },
            { file: 'lease_contract_2026.jpg', ref: 'JV-812', size: '1.1 MB' },
          ].map((doc) => (
            <div key={doc.file} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <FileText className="w-4 h-4 text-[#C89D42]" />
                <div>
                  <div className="font-mono-num font-bold">{doc.file}</div>
                  <div className="text-[10px] text-slate-500">{isAr ? 'مرتبط بـ' : 'Linked to'} {doc.ref}</div>
                </div>
              </div>
              <span className="font-mono-num text-[10px] text-slate-500">{doc.size}</span>
            </div>
          ))}
        </div>
      );

    case 'branches':
      return (
        <div className="p-4 space-y-2.5">
          {[
            { name: isAr ? 'الفرع الرئيسي — الوسط' : 'HQ Branch — Central', rev: '64,200.00' },
            { name: isAr ? 'فرع الشمال' : 'North Branch', rev: '41,800.00' },
          ].map((br) => (
            <div key={br.name} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <MapPin className="w-4 h-4 text-[#C89D42]" />
                <span className="font-bold">{br.name}</span>
              </div>
              <span className="font-mono-num font-bold text-[#15803D] dark:text-[#22C55E]">{br.rev} {cur}</span>
            </div>
          ))}
        </div>
      );

    case 'projects':
      return (
        <div className="p-4 space-y-2.5">
          {[
            { name: isAr ? 'مشروع تجهيز الموقع الإداري' : 'HQ Fit-out Project', prog: '75%', margin: '+18,400' },
            { name: isAr ? 'عقد الصيانة السنوي B2B' : 'Annual B2B Maintenance', prog: '40%', margin: '+9,200' },
          ].map((pr) => (
            <div key={pr.name} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-1 text-xs">
              <div className="flex items-center justify-between font-bold">
                <span className="flex items-center gap-1.5"><FolderKanban className="w-3.5 h-3.5 text-[#C89D42]" /> {pr.name}</span>
                <span className="font-mono-num">{pr.prog}</span>
              </div>
              <div className="text-[11px] font-mono-num text-[#15803D] dark:text-[#22C55E]">{isAr ? 'الربح المرحلي:' : 'Margin:'} {pr.margin} {cur}</div>
            </div>
          ))}
        </div>
      );

    case 'manufacturing':
      return (
        <div className="p-4 space-y-2.5">
          <div className="p-3 rounded-xl bg-[#0B192C] text-white text-xs space-y-1">
            <div className="flex items-center gap-1.5 font-bold text-[#D9B15B]">
              <Factory className="w-4 h-4" />
              <span>{isAr ? 'أمر إنتاج #MFG-204 (قائمة BOM)' : 'Production Run #MFG-204 (BOM)'}</span>
            </div>
            <p className="text-[11px] text-slate-300">{isAr ? 'خصم 3 مواد خام ← إضافة 50 وحدة تامة الصنع' : 'Deducts 3 raw materials -> Adds 50 finished units'}</p>
          </div>
          <button
            type="button"
            onClick={() => onNavigate('inventory')}
            className="w-full min-h-[42px] rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-bold"
          >
            {isAr ? 'مراجعة مخزون المواد الخام' : 'Inspect Raw Materials Stock'}
          </button>
        </div>
      );

    case 'restaurants':
      return (
        <div className="p-4 space-y-3">
          <div className="grid grid-cols-2 gap-2">
            {[
              { t: isAr ? 'طاولة 01' : 'Table 01', st: isAr ? 'مشغولة · 185' : 'Occupied · 185', busy: true },
              { t: isAr ? 'طاولة 02' : 'Table 02', st: isAr ? 'شاغرة' : 'Available', busy: false },
              { t: isAr ? 'طاولة 03' : 'Table 03', st: isAr ? 'تجهيز المطبخ' : 'Kitchen Prep', busy: true },
              { t: isAr ? 'سفري #48' : 'Takeaway #48', st: isAr ? 'جاهز للتسليم' : 'Ready', busy: false },
            ].map((tb) => (
              <div key={tb.t} className={`p-3 rounded-xl border text-xs ${tb.busy ? 'border-[#C89D42] bg-[#C89D42]/10' : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33]'}`}>
                <div className="font-bold flex items-center gap-1.5"><UtensilsCrossed className="w-3.5 h-3.5 text-[#C89D42]" />{tb.t}</div>
                <div className="text-[11px] text-slate-500 mt-1">{tb.st}</div>
              </div>
            ))}
          </div>
        </div>
      );

    case 'pharmacies':
      return (
        <div className="p-4 space-y-2.5">
          {[
            { med: isAr ? 'مستحضر دوائي A — تشغيلة B402' : 'Pharma Item A — Batch B402', exp: '2026-12-15', warn: true },
            { med: isAr ? 'مستحضر دوائي B — تشغيلة B910' : 'Pharma Item B — Batch B910', exp: '2028-03-01', warn: false },
          ].map((m) => (
            <div key={m.med} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-1 text-xs">
              <div className="flex items-center gap-1.5 font-bold">
                <Pill className="w-3.5 h-3.5 text-[#C89D42]" />
                <span>{m.med}</span>
              </div>
              <div className={`font-mono-num text-[11px] ${m.warn ? 'text-[#DC2626] dark:text-[#F87171] font-bold' : 'text-slate-500'}`}>
                {isAr ? 'انتهاء الصلاحية:' : 'Expiry:'} {m.exp} {m.warn ? (isAr ? '(ينتهي خلال 30 يوماً)' : '(Expiring <30d)') : ''}
              </div>
            </div>
          ))}
        </div>
      );

    case 'workshops':
      return (
        <div className="p-4 space-y-2.5">
          {[
            { job: 'WO-310', desc: isAr ? 'صيانة شاملة + تغيير قطع غيار' : 'Full Repair + Spare Parts', stage: isAr ? 'قيد الإصلاح' : 'In Repair' },
            { job: 'WO-309', desc: isAr ? 'فحص دوري وتبديل وحدة' : 'Periodic Inspection', stage: isAr ? 'جاهز للفوترة' : 'Ready to Bill' },
          ].map((w) => (
            <div key={w.job} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex items-center justify-between text-xs">
              <div>
                <div className="font-mono-num font-bold text-[#9A7222] dark:text-[#D9B15B] flex items-center gap-1"><Wrench className="w-3.5 h-3.5" />{w.job}</div>
                <div className="mt-0.5 font-semibold">{w.desc}</div>
              </div>
              <span className="text-[11px] font-bold">{w.stage}</span>
            </div>
          ))}
        </div>
      );

    case 'shipping':
      return (
        <div className="p-4 space-y-2.5">
          {[
            { id: 'SHP-771', driver: isAr ? 'مندوب: خالد فهد' : 'Courier: Khaled', cod: '340.00', st: isAr ? 'قيد التوصيل' : 'Out for Delivery' },
            { id: 'SHP-770', driver: isAr ? 'مندوب: طارق علي' : 'Courier: Tariq', cod: '190.00', st: isAr ? 'تم التسليم والتحصيل' : 'Delivered & COD Paid' },
          ].map((s) => (
            <div key={s.id} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-1 text-xs">
              <div className="flex justify-between font-bold">
                <span className="flex items-center gap-1.5"><Truck className="w-3.5 h-3.5 text-[#C89D42]" />{s.id}</span>
                <span className="font-mono-num">{s.cod} {cur} COD</span>
              </div>
              <div className="text-[11px] text-slate-500">{s.driver} · {s.st}</div>
            </div>
          ))}
        </div>
      );

    case 'loyalty':
      return (
        <div className="p-4 space-y-2.5">
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-1 text-xs">
            <div className="flex items-center gap-1.5 font-bold"><Gift className="w-4 h-4 text-[#C89D42]" /><span>{isAr ? 'برنامج النقاط الذهبي' : 'Gold Points Tier'}</span></div>
            <p className="text-[11px] text-slate-500">{isAr ? 'نقطة لكل 10 ' + cur + ' · الاستبدال يرحّل لحساب خصم مسموح به.' : '1 pt per 10 ' + cur + ' · Redemption posts to Allowed Discount.'}</p>
          </div>
        </div>
      );

    case 'taxes-currencies':
      return (
        <div className="p-4 space-y-2.5 text-xs">
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex justify-between">
            <span className="font-bold">{isAr ? 'ضريبة القيمة المضافة القياسية' : 'Standard VAT Rate'}</span>
            <span className="font-mono-num font-bold text-[#9A7222] dark:text-[#D9B15B]">15.00%</span>
          </div>
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] flex justify-between">
            <span className="font-bold">USD → {cur}</span>
            <span className="font-mono-num font-bold">{cur === 'SAR' ? '3.7500' : cur === 'AED' ? '3.6725' : cur === 'EGP' ? '49.2000' : '1.0000'}</span>
          </div>
        </div>
      );

    case 'reports':
      return (
        <div className="p-4 space-y-3">
          <button
            type="button"
            onClick={() => setAuditRan(true)}
            className="w-full min-h-[42px] rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold"
          >
            {isAr ? 'تشغيل نظام الفحص والمراجعة الشامل' : 'Run 9-Point Diagnostic Audit'}
          </button>
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-2 text-[11px]">
            {[
              [isAr ? 'القيود غير المتوازنة' : 'Unbalanced Journals', '0'],
              [isAr ? 'تطابق الأرصدة وقاعدة البيانات' : 'Balance & DB Integrity', auditRan ? (isAr ? 'سليم' : 'Verified') : '—'],
              [isAr ? 'الفواتير والعملاء المكررون' : 'Duplicate Invoices/Parties', auditRan ? (isAr ? '1 تشابه' : '1 Similar') : '—'],
              [isAr ? 'أخطاء المخزون والحسابات غير المكتملة' : 'Stock & Incomplete Accounts', auditRan ? '0' : '—'],
            ].map(([k, v]) => (
              <div key={k} className="flex justify-between">
                <span>{k}</span>
                <span className="font-mono-num font-bold text-[#15803D] dark:text-[#22C55E]">{v}</span>
              </div>
            ))}
          </div>
          <div className="grid grid-cols-2 gap-2">
            <button type="button" onClick={() => onNavigate('trial-balance')} className="min-h-[40px] rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-bold">
              {isAr ? 'ميزان المراجعة' : 'Trial Balance'}
            </button>
            <button type="button" onClick={() => onNavigate('financial-statements')} className="min-h-[40px] rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-bold">
              {isAr ? 'القوائم المالية' : 'Statements'}
            </button>
          </div>
        </div>
      );

    case 'ai-assistant': {
      const prompts = [
        {
          q: isAr ? 'لخّص الوضع المالي للنشاط' : 'Summarize activity financial position',
          a: isAr
            ? `استناداً إلى القيود المرحّلة في (${business.activityName.ar}): المبيعات 84,200 ${cur}، المصروفات 12,800 ${cur}، وصافي الربح 21,900 ${cur}. لا توجد أرقام مقدّرة أو معدّلة.`
            : `From posted journals in ${business.activityName.en}: Sales 84,200 ${cur}, Expenses 12,800 ${cur}, Net Profit 21,900 ${cur}. Zero invented numbers.`,
        },
        {
          q: isAr ? 'هل توجد أنماط غير طبيعية؟' : 'Any unusual anomalies?',
          a: isAr
            ? 'ارتفعت مصروفات الصيانة بنسبة 28% مقارنة بمتوسط 3 أشهر. لم يتم تعديل أي قيد تلقائياً.'
            : 'Maintenance expenses rose 28% vs. 3-month baseline. No entries modified automatically.',
        },
      ];
      const activePrompt = prompts[aiQueryIndex % prompts.length];
      return (
        <div className="p-4 space-y-3">
          <div className="p-3 rounded-xl bg-[#0B192C] text-white text-xs space-y-1">
            <div className="flex items-center gap-1.5 text-[#D9B15B] font-bold">
              <Sparkles className="w-4 h-4" />
              <span>{isAr ? 'المحاسب الذكي المحلي (Offline)' : 'Offline Smart Assistant'}</span>
            </div>
            <p className="text-[11px] text-slate-300">{isAr ? 'تحليل محلي بالعربية دون اتصال إلزامي أو تعديل تلقائي.' : 'Local analysis without external API or auto-edits.'}</p>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {prompts.map((p, idx) => (
              <button
                key={p.q}
                type="button"
                onClick={() => setAiQueryIndex(idx)}
                className={`px-2.5 py-1.5 rounded-lg text-[11px] font-semibold border ${
                  aiQueryIndex % prompts.length === idx ? 'border-[#C89D42] bg-[#C89D42]/15' : 'border-slate-200 dark:border-slate-700'
                }`}
              >
                {p.q}
              </button>
            ))}
          </div>
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] text-xs leading-relaxed">
            {activePrompt.a}
          </div>
        </div>
      );
    }

    case 'backup':
      return (
        <div className="p-4 space-y-3">
          <div className="p-3 rounded-xl bg-[#0B192C] text-white text-xs space-y-1">
            <div className="font-bold text-[#D9B15B]">{isAr ? 'النسخ التلقائي المجدول (WorkManager)' : 'Scheduled Backup (WorkManager)'}</div>
            <div className="text-[11px] text-slate-300 font-mono-num">{business.dbFileName} · AES-256</div>
          </div>
          <button
            type="button"
            onClick={() => setBackupVerified(true)}
            className="w-full min-h-[44px] rounded-xl bg-[#C89D42] text-[#0B192C] text-xs font-bold"
          >
            {isAr ? 'إنشاء نسخة + اختبار الاستعادة الفعلي' : 'Create Snapshot & Test Restore'}
          </button>
          {backupVerified && (
            <div className="p-3 rounded-xl border border-emerald-200 dark:border-emerald-900 bg-emerald-50/70 dark:bg-emerald-950/30 text-xs text-[#15803D] dark:text-[#22C55E] font-semibold">
              {isAr ? 'تم فحص النسخة المشفرة وتأكيد سلامتها 100%.' : 'Encrypted archive verified 100% intact.'}
            </div>
          )}
          <button type="button" onClick={() => onNavigate('restore')} className="w-full min-h-[42px] rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-bold">
            {isAr ? 'الانتقال لشاشة الاستعادة' : 'Open Restore Screen'}
          </button>
        </div>
      );

    case 'security':
      return (
        <div className="p-4 space-y-3">
          <div className="p-3 rounded-xl border border-emerald-200 dark:border-emerald-900 bg-emerald-50/70 dark:bg-emerald-950/20 flex items-center gap-2 text-xs font-bold text-[#15803D] dark:text-[#22C55E]">
            <ShieldCheck className="w-4 h-4" />
            <span>{isAr ? 'SQLCipher + Android Keystore نشط' : 'SQLCipher + Keystore Active'}</span>
          </div>
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-2 text-xs">
            <div className="flex justify-between">
              <span className="font-bold">{isAr ? 'مفتاح الاسترداد الخارجي' : 'Recovery Key'}</span>
              <button type="button" onClick={() => setShowRecoveryKey((v) => !v)} className="text-[#9A7222] dark:text-[#D9B15B] font-bold">
                {showRecoveryKey ? (isAr ? 'إخفاء' : 'Hide') : (isAr ? 'عرض' : 'Reveal')}
              </button>
            </div>
            <div className="font-mono-num text-[11px]">{showRecoveryKey ? 'DFTR-9F4A-88C2-7E10-B3A9' : '•••• - •••• - •••• - B3A9'}</div>
          </div>
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-1.5 text-[11px]">
            <div className="font-bold text-xs">{isAr ? 'سجل التدقيق غير القابل للتعديل' : 'Immutable Audit Log'}</div>
            <div className="flex justify-between"><span>{isAr ? 'دخول ناجح (المدير)' : 'Login OK'}</span><span className="font-mono-num">10:14</span></div>
            <div className="flex justify-between text-[#DC2626] dark:text-[#F87171]"><span>{isAr ? 'محاولة PIN فاشلة' : 'Failed PIN'}</span><span className="font-mono-num">09:52</span></div>
          </div>
        </div>
      );

    case 'settings':
      return (
        <div className="p-4 space-y-3 text-xs">
          <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] space-y-3">
            <div className="flex justify-between items-center">
              <span className="font-bold">{isAr ? 'اللغة (RTL / LTR)' : 'Language'}</span>
              <button type="button" onClick={onToggleLang} className="px-3 py-1.5 rounded-lg bg-slate-100 dark:bg-[#0B192C] font-bold">
                {isAr ? 'العربية' : 'English'}
              </button>
            </div>
            <div className="flex justify-between items-center">
              <span className="font-bold">{isAr ? 'المظهر (نهاري / ليلي)' : 'Theme'}</span>
              <button type="button" onClick={onToggleTheme} className="px-3 py-1.5 rounded-lg bg-slate-100 dark:bg-[#0B192C] font-bold">
                {theme === 'dark' ? (isAr ? 'ليلي' : 'Dark') : (isAr ? 'نهاري' : 'Light')}
              </button>
            </div>
            <div className="flex justify-between items-center">
              <span className="font-bold">{isAr ? 'القطاع والوحدات' : 'Sector Modules'}</span>
              <button type="button" onClick={() => onNavigate('business-type')} className="text-[#9A7222] dark:text-[#D9B15B] font-bold">
                {business.name[lang]}
              </button>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <button type="button" onClick={() => onNavigate('taxes-currencies')} className="min-h-[40px] rounded-xl border border-slate-200 dark:border-slate-700 font-bold">
              {isAr ? 'الضرائب والعملات' : 'Taxes & FX'}
            </button>
            <button type="button" onClick={() => onNavigate('security')} className="min-h-[40px] rounded-xl border border-slate-200 dark:border-slate-700 font-bold">
              {isAr ? 'الحماية والتدقيق' : 'Security Vault'}
            </button>
          </div>
        </div>
      );

    default:
      return null;
  }
};
