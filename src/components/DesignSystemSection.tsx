import React, { useState } from 'react';
import { Check, Copy, AlertTriangle, CheckCircle2, ShieldCheck } from 'lucide-react';
import { COLOR_TOKENS } from '../data/daftarSpec';
import { Language, ThemeMode } from '../types/daftar';

interface DesignSystemSectionProps {
  lang: Language;
  theme: ThemeMode;
}

export const DesignSystemSection: React.FC<DesignSystemSectionProps> = ({ lang, theme }) => {
  const isAr = lang === 'ar';
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [sampleMethod, setSampleMethod] = useState<'fifo' | 'wac'>('wac');

  const handleCopy = (id: string, hex: string) => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(hex).catch(() => {});
    }
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  return (
    <section
      id="design-system"
      className="py-16 border-t border-slate-200 dark:border-slate-800/80"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
          <div className="max-w-3xl">
            <p className="text-xs font-semibold tracking-wide text-[#9A7222] dark:text-[#D9B15B] mb-2">
              {isAr ? '02. نظام التصميم ومعايير Jetpack Compose' : '02. Design System & Jetpack Compose Tokens'}
            </p>
            <h2 className="text-2xl sm:text-3xl font-bold text-[#0B192C] dark:text-slate-100 leading-tight [text-wrap:balance]">
              {isAr
                ? 'ألوان منضبطة، أرقام جدولية دقيقة، وتباين واضح نهاراً وليلاً'
                : 'Disciplined 60-30-10 Palette, Tabular Numerals & Dual-Theme Precision'}
            </h2>
          </div>
          <div className="text-xs text-slate-500 dark:text-slate-400 whitespace-nowrap">
            {isAr
              ? 'الوضع المعروض: ' + (theme === 'dark' ? 'الوضع الليلي' : 'الوضع النهاري')
              : 'Active Theme: ' + (theme === 'dark' ? 'Dark Mode' : 'Light Mode')}
          </div>
        </div>

        {/* Color Tokens Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-10">
          {COLOR_TOKENS.map((token) => {
            const activeHex = theme === 'dark' ? token.darkHex : token.lightHex;
            const isCopied = copiedId === token.id;
            return (
              <div
                key={token.id}
                className="bg-white dark:bg-[#0F1E33] rounded-xl border border-slate-200 dark:border-slate-800 p-5 flex flex-col justify-between gap-4"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div
                      className="w-12 h-12 rounded-xl border border-slate-200 dark:border-slate-700 shrink-0"
                      style={{ backgroundColor: activeHex }}
                    />
                    <div>
                      <h3 className="text-sm font-bold text-[#0B192C] dark:text-slate-100">
                        {token.name[lang]}
                      </h3>
                      <p className="font-mono-num text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                        {token.composeToken}
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleCopy(token.id, activeHex)}
                    className="min-h-[36px] px-2.5 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 text-xs font-mono-num text-slate-700 dark:text-slate-200 hover:border-[#C89D42] transition-colors flex items-center gap-1.5 shrink-0"
                    title={isAr ? 'نسخ رمز اللون' : 'Copy hex code'}
                  >
                    {isCopied ? (
                      <Check className="w-3.5 h-3.5 text-[#15803D] dark:text-[#22C55E]" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 opacity-60" />
                    )}
                    <span>{activeHex}</span>
                  </button>
                </div>
                <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                  {token.usage[lang]}
                </p>
              </div>
            );
          })}
        </div>

        {/* Typography & Live Compose Component Specimens */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
          {/* Typography Scale */}
          <div className="lg:col-span-5 bg-white dark:bg-[#0F1E33] rounded-2xl border border-slate-200 dark:border-slate-800 p-6 flex flex-col justify-between gap-6">
            <div>
              <h3 className="text-base font-bold text-[#0B192C] dark:text-slate-100 mb-4">
                {isAr ? 'سلم الخطوط والأرقام الجدولية' : 'Typography & Tabular Figure Scale'}
              </h3>
              <div className="space-y-4 divide-y divide-slate-100 dark:divide-slate-800">
                <div className="pt-1">
                  <div className="text-xs text-slate-500 dark:text-slate-400 mb-1">
                    Cairo Bold · Display / Headings (24sp–30sp)
                  </div>
                  <div className="text-2xl font-bold text-[#0B192C] dark:text-slate-100">
                    {isAr ? 'صافي الدخل الفصلي' : 'Quarterly Net Income'}
                  </div>
                </div>
                <div className="pt-4">
                  <div className="text-xs text-slate-500 dark:text-slate-400 mb-1">
                    IBM Plex Sans Arabic · Body & Forms (14sp–16sp)
                  </div>
                  <p className="text-sm text-slate-700 dark:text-slate-300 leading-relaxed">
                    {isAr
                      ? 'قيد مبيعات آجل رقم INV-1042 مرحّل تلقائياً إلى حساب العملاء وضريبة القيمة المضافة.'
                      : 'Credit sales journal INV-1042 automatically posted to Receivables and VAT Output.'}
                  </p>
                </div>
                <div className="pt-4">
                  <div className="text-xs text-slate-500 dark:text-slate-400 mb-1">
                    IBM Plex Mono · Tabular Figures (13sp–22sp)
                  </div>
                  <div className="grid grid-cols-2 gap-3 font-mono-num text-sm">
                    <div className="p-2.5 rounded-lg bg-slate-50 dark:bg-[#0B192C] border border-slate-200 dark:border-slate-800">
                      <span className="text-xs text-slate-500 block">{isAr ? 'مدين' : 'Debit'}</span>
                      <span className="font-semibold text-[#0B192C] dark:text-slate-100">14,850.00</span>
                    </div>
                    <div className="p-2.5 rounded-lg bg-slate-50 dark:bg-[#0B192C] border border-slate-200 dark:border-slate-800">
                      <span className="text-xs text-slate-500 block">{isAr ? 'دائن' : 'Credit'}</span>
                      <span className="font-semibold text-[#15803D] dark:text-[#22C55E]">14,850.00</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div className="text-xs text-slate-500 dark:text-slate-400 pt-3 border-t border-slate-100 dark:border-slate-800">
              {isAr
                ? 'مقاس اللمس الأدنى في Android: 48dp × 48dp · استجابة بصرية خلال ≤ 150ms'
                : 'Android Min Touch Target: 48dp × 48dp · Interaction settle ≤ 150ms'}
            </div>
          </div>

          {/* Interactive Component Tokens Specimen */}
          <div className="lg:col-span-7 bg-white dark:bg-[#0F1E33] rounded-2xl border border-slate-200 dark:border-slate-800 p-6 flex flex-col justify-between gap-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-bold text-[#0B192C] dark:text-slate-100">
                {isAr ? 'مكونات الواجهة الدلالية (Material 3 + Compose)' : 'Semantic UI Components (Material 3 + Compose)'}
              </h3>
              <div className="inline-flex items-center gap-1 p-1 rounded-lg bg-slate-100 dark:bg-[#0B192C] border border-slate-200 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => setSampleMethod('wac')}
                  className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors whitespace-nowrap ${
                    sampleMethod === 'wac'
                      ? 'bg-[#0B192C] text-white dark:bg-[#C89D42] dark:text-[#0B192C]'
                      : 'text-slate-600 dark:text-slate-300'
                  }`}
                >
                  {isAr ? 'المتوسط المرجح' : 'Weighted Avg'}
                </button>
                <button
                  type="button"
                  onClick={() => setSampleMethod('fifo')}
                  className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors whitespace-nowrap ${
                    sampleMethod === 'fifo'
                      ? 'bg-[#0B192C] text-white dark:bg-[#C89D42] dark:text-[#0B192C]'
                      : 'text-slate-600 dark:text-slate-300'
                  }`}
                >
                  {isAr ? 'الوارد أولاً FIFO' : 'FIFO Costing'}
                </button>
              </div>
            </div>

            {/* Semantic States: Color + Explicit Icon + Text (Never color alone) */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="p-3.5 rounded-xl border border-emerald-200 dark:border-emerald-900/60 bg-emerald-50/70 dark:bg-emerald-950/30">
                <div className="flex items-center gap-2 text-[#15803D] dark:text-[#22C55E] font-semibold text-xs">
                  <CheckCircle2 className="w-4 h-4 shrink-0" />
                  <span>{isAr ? 'قيد متوازن معتمد' : 'Balanced & Posted'}</span>
                </div>
                <p className="font-mono-num text-xs text-slate-700 dark:text-slate-300 mt-2">
                  Δ = 0.00 SAR
                </p>
              </div>

              <div className="p-3.5 rounded-xl border border-amber-200 dark:border-amber-900/60 bg-amber-50/70 dark:bg-amber-950/30">
                <div className="flex items-center gap-2 text-[#B45309] dark:text-[#FBBF24] font-semibold text-xs">
                  <ShieldCheck className="w-4 h-4 shrink-0" />
                  <span>{isAr ? 'تنبيه حد المخزون' : 'Low Stock Warning'}</span>
                </div>
                <p className="text-xs text-slate-700 dark:text-slate-300 mt-2">
                  {sampleMethod === 'wac'
                    ? isAr ? 'التكلفة: متوسط مرجح' : 'Method: Weighted Avg'
                    : isAr ? 'التكلفة: FIFO دقيق' : 'Method: Strict FIFO'}
                </p>
              </div>

              <div className="p-3.5 rounded-xl border border-red-200 dark:border-red-900/60 bg-red-50/70 dark:bg-red-950/30">
                <div className="flex items-center gap-2 text-[#DC2626] dark:text-[#F87171] font-semibold text-xs">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  <span>{isAr ? 'قيد غير متوازن مرفوض' : 'Unbalanced Blocked'}</span>
                </div>
                <p className="font-mono-num text-xs text-slate-700 dark:text-slate-300 mt-2">
                  {isAr ? 'الفرق: 120.00' : 'Diff: 120.00'}
                </p>
              </div>
            </div>

            {/* Action Hierarchy Specimen */}
            <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex flex-wrap items-center justify-between gap-4">
              <div className="flex flex-wrap items-center gap-3">
                <span className="inline-flex items-center justify-center min-h-[44px] px-5 py-2.5 rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold whitespace-nowrap">
                  {isAr ? 'اعتماد القيد المزدوج' : 'Post Journal Entry'}
                </span>
                <span className="inline-flex items-center justify-center min-h-[44px] px-5 py-2.5 rounded-xl border border-[#C89D42] text-[#9A7222] dark:text-[#D9B15B] text-xs font-bold whitespace-nowrap">
                  {isAr ? 'قيد عكسي' : 'Create Reversal'}
                </span>
                <span className="inline-flex items-center justify-center min-h-[44px] px-4 py-2.5 rounded-xl text-slate-600 dark:text-slate-300 text-xs font-semibold whitespace-nowrap">
                  {isAr ? 'حفظ كمسودة' : 'Save Draft'}
                </span>
              </div>
              <span className="text-xs font-mono-num text-slate-500 dark:text-slate-400">
                RoundedCornerShape(12.dp) · 1.dp Hairline
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
