import React from 'react';

interface DaftarLogoProps {
  variant?: 'navy-gold' | 'gold-on-navy' | 'monochrome';
  size?: number;
  className?: string;
}

/**
 * Original Geometric Emblem for DAFTAR (دفتر):
 * Combines a chamfered sovereign ledger frame, two balanced double-entry
 * folio columns (Debit = Credit), and a precision golden nib apex.
 */
export const DaftarLogo: React.FC<DaftarLogoProps> = ({
  variant = 'navy-gold',
  size = 48,
  className = '',
}) => {
  const isDarkBg = variant === 'gold-on-navy';
  const isMono = variant === 'monochrome';

  const bgFill = isDarkBg ? '#0B192C' : isMono ? '#0B192C' : '#0B192C';
  const borderStroke = isMono ? '#E2E8F0' : '#C89D42';
  const folioStroke = isMono ? '#FFFFFF' : '#F8FAFC';
  const goldFill = isMono ? '#FFFFFF' : '#C89D42';

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      role="img"
      aria-label="شعار دفتر — المحاسب الذكي | DAFTAR Logo"
    >
      {/* Chamfered Sovereign Frame */}
      <path
        d="M14 4H50L60 14V50L50 60H14L4 50V14L14 4Z"
        fill={bgFill}
        stroke={borderStroke}
        strokeWidth="2"
      />
      {/* Inner Hairline Architectural Frame */}
      <path
        d="M16.5 8H47.5L56 16.5V47.5L47.5 56H16.5L8 47.5V16.5L16.5 8Z"
        stroke={borderStroke}
        strokeOpacity="0.32"
        strokeWidth="1"
      />

      {/* Open Ledger Folio Wings (Left = Debit, Right = Credit) */}
      <path
        d="M15 19.5C15 18.1193 16.1193 17 17.5 17H29C30.6569 17 32 18.3431 32 20V46C30.4 44.8 28.4 44.2 26.2 44.2H17.5C16.1193 44.2 15 43.0807 15 41.7V19.5Z"
        stroke={folioStroke}
        strokeWidth="2.2"
        strokeLinejoin="round"
      />
      <path
        d="M49 19.5C49 18.1193 47.8807 17 46.5 17H35C33.3431 17 32 18.3431 32 20V46C33.6 44.8 35.6 44.2 37.8 44.2H46.5C47.8807 44.2 49 43.0807 49 41.7V19.5Z"
        stroke={folioStroke}
        strokeWidth="2.2"
        strokeLinejoin="round"
      />

      {/* Balanced Entry Lines inside Left & Right Folios */}
      <line x1="19.5" y1="24" x2="27.5" y2="24" stroke={goldFill} strokeWidth="2" strokeLinecap="round" />
      <line x1="19.5" y1="30" x2="27.5" y2="30" stroke={folioStroke} strokeOpacity="0.7" strokeWidth="1.8" strokeLinecap="round" />
      <line x1="19.5" y1="36" x2="25.5" y2="36" stroke={folioStroke} strokeOpacity="0.7" strokeWidth="1.8" strokeLinecap="round" />

      <line x1="36.5" y1="24" x2="44.5" y2="24" stroke={goldFill} strokeWidth="2" strokeLinecap="round" />
      <line x1="36.5" y1="30" x2="44.5" y2="30" stroke={folioStroke} strokeOpacity="0.7" strokeWidth="1.8" strokeLinecap="round" />
      <line x1="38.5" y1="36" x2="44.5" y2="36" stroke={folioStroke} strokeOpacity="0.7" strokeWidth="1.8" strokeLinecap="round" />

      {/* Central Golden Balance Nib / Arch Apex */}
      <path
        d="M32 12L35.5 16.5L32 20L28.5 16.5L32 12Z"
        fill={goldFill}
      />
      <circle cx="32" cy="49.5" r="2.2" fill={goldFill} />
    </svg>
  );
};
