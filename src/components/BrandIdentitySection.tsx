import React from 'react';
import { Language } from '../types/daftar';
import { DaftarLogo } from './DaftarLogo';

interface BrandIdentitySectionProps {
  lang: Language;
}

export const BrandIdentitySection: React.FC<BrandIdentitySectionProps> = ({ lang }) => {
  const isAr = lang === 'ar';

  const pillars = [
    {
      index: '01',
      title: isAr ? 'توازن القيد المزدوج' : 'Double-Entry Symmetry',
      desc: isAr
        ? 'تمثل الصفحتان المتقابلتان في الشعار تساوي المدين والدائن في كل عملية مالية دون استثناء.'
        : 'Opposing ledger folios symbolize strict Debit = Credit symmetry enforced on every transaction.',
    },
    {
      index: '02',
      title: isAr ? 'السيادة والعمل دون إنترنت' : 'Offline-First Sovereignty',
      desc: isAr
        ? 'الإطار الهندسي المثمّن يعكس حماية قاعدة SQLCipher محلياً بمفاتيح Android Keystore دون خادم خارجي.'
        : 'The chamfered octagonal seal conveys local SQLCipher storage guarded by Android Keystore with zero cloud dependency.',
    },
    {
      index: '03',
      title: isAr ? 'فخامة الكحلي والذهبي' : 'Navy & Burnished Gold',
      desc: isAr
        ? 'الكحلي الداكن يمنح الثقة والوقار البنكي، والذهبي يبرز نقاط القرار والربحية بوضوح غير صاخب.'
        : 'Deep Navy establishes institutional trust while Burnished Gold directs focus to primary financial actions.',
    },
  ];

  return (
    <section
      id="brand-identity"
      className="py-16 border-t border-slate-200 dark:border-slate-800/80"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-3xl mb-10">
          <p className="text-xs font-semibold tracking-wide text-[#9A7222] dark:text-[#D9B15B] mb-2">
            {isAr ? '01. الهوية البصرية والشعار الأصلي' : '01. Visual Identity & Original Emblem'}
          </p>
          <h2 className="text-2xl sm:text-3xl font-bold text-[#0B192C] dark:text-slate-100 leading-tight [text-wrap:balance]">
            {isAr
              ? 'هوية مالية وقورة تجمع دقة الدفاتر الكلاسيكية وحداثة Android'
              : 'Authoritative Financial Identity Blending Ledger Precision with Modern Android Craft'}
          </h2>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
          {/* Primary Logo Presentation Canvas */}
          <div className="lg:col-span-7 bg-[#0B192C] text-white rounded-2xl p-8 sm:p-10 flex flex-col justify-between border border-slate-800">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-white/10 pb-6">
              <div className="flex items-center gap-4">
                <DaftarLogo variant="gold-on-navy" size={72} />
                <div>
                  <div className="text-3xl font-extrabold tracking-tight text-white">
                    دفتر <span className="text-[#D9B15B] font-semibold text-xl mx-1">|</span> DAFTAR
                  </div>
                  <p className="text-sm text-slate-300 mt-1">
                    {isAr ? 'المحاسب الذكي · نظام محاسبي وإداري مستقل' : 'Smart Accounting System · Offline-First'}
                  </p>
                </div>
              </div>
              <div className="text-xs font-mono-num text-slate-400">
                1:1 Vector · Adaptive Android Icon
              </div>
            </div>

            {/* Variants Strip */}
            <div className="grid grid-cols-3 gap-4 pt-8">
              <div className="bg-white rounded-xl p-4 flex flex-col items-center justify-center gap-2 text-center">
                <DaftarLogo variant="navy-gold" size={44} />
                <span className="text-xs font-medium text-[#0B192C]">
                  {isAr ? 'الوضع النهاري' : 'Light Surface'}
                </span>
              </div>
              <div className="bg-[#132642] rounded-xl p-4 flex flex-col items-center justify-center gap-2 text-center border border-white/10">
                <DaftarLogo variant="gold-on-navy" size={44} />
                <span className="text-xs font-medium text-slate-100">
                  {isAr ? 'الوضع الليلي' : 'Dark Surface'}
                </span>
              </div>
              <div className="bg-slate-900 rounded-xl p-4 flex flex-col items-center justify-center gap-2 text-center border border-white/10">
                <DaftarLogo variant="monochrome" size={44} />
                <span className="text-xs font-medium text-slate-300">
                  {isAr ? 'أحادي الطباعة' : 'Thermal / Print'}
                </span>
              </div>
            </div>
          </div>

          {/* Logo Meaning & Architecture Pillars */}
          <div className="lg:col-span-5 bg-white dark:bg-[#0F1E33] rounded-2xl p-6 sm:p-8 border border-slate-200 dark:border-slate-800 flex flex-col justify-between gap-6">
            <div>
              <h3 className="text-lg font-bold text-[#0B192C] dark:text-slate-100 mb-5">
                {isAr ? 'فلسفة التكوين البصري' : 'Emblem & Identity Principles'}
              </h3>
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                {pillars.map((p) => (
                  <div key={p.index} className="py-4 first:pt-0 last:pb-0">
                    <div className="flex items-baseline gap-2.5">
                      <span className="font-mono-num text-xs font-semibold text-[#C89D42]">
                        {p.index}.
                      </span>
                      <h4 className="text-sm font-bold text-[#0B192C] dark:text-slate-100">
                        {p.title}
                      </h4>
                    </div>
                    <p className="text-sm text-slate-600 dark:text-slate-400 mt-1 leading-relaxed">
                      {p.desc}
                    </p>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-4 border-t border-slate-100 dark:border-slate-800 text-xs text-slate-500 dark:text-slate-400 flex flex-wrap items-center gap-x-2 gap-y-1">
              <span>Kotlin</span>
              <span aria-hidden="true">·</span>
              <span>Jetpack Compose</span>
              <span aria-hidden="true">·</span>
              <span>Room + SQLCipher</span>
              <span aria-hidden="true">·</span>
              <span>Android 8.0+</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
