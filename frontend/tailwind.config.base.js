// 프론트엔드 공통 tailwind.config.js 파일
// tailwind.config.base.js
/** @type {import('tailwindcss').Config} */
module.exports = {
    theme: {
      extend: {
        // 1. 피그마에서 추출한 Pretendard 기반 타이포그래피 (typography.json 반영)
        fontSize: {
          // Display & Headings
          'h1': ['31px', { lineHeight: '37.2px', fontWeight: '400' }],
          'h1-bold': ['31px', { lineHeight: '37.2px', fontWeight: '700' }],
          'h2': ['25px', { lineHeight: '30px', fontWeight: '400' }],
          'h2-bold': ['25px', { lineHeight: '30px', fontWeight: '700' }],
  
          // Body Texts
          'body-lg': ['20px', { lineHeight: '24px', fontWeight: '400' }],
          'body-lg-bold': ['20px', { lineHeight: '24px', fontWeight: '700' }],
          'body-main': ['16px', { lineHeight: '19.2px', fontWeight: '400' }],
          'body-main-bold': ['16px', { lineHeight: '19.2px', fontWeight: '700' }],
          'body-sm': ['13px', { lineHeight: '15.6px', fontWeight: '400' }],
          'body-sm-bold': ['13px', { lineHeight: '15.6px', fontWeight: '700' }],
  
          // Captions
          'caption': ['10px', { lineHeight: '12px', fontWeight: '400' }],
          'caption-bold': ['10px', { lineHeight: '12px', fontWeight: '700' }],
        },
  
        // 2. '지식의 숲' 컬러 시스템 (colors.tokens.json 반영)
        colors: {
          'forest-bg': '#101417',       // background
          'action-accent': '#4ade80',   // action / CTA
          'action-accent-hover': '#3fce75',
          'action-accent-active': '#33b968',
          'primary-signal': '#00ffc2',  // primary_signal / normal
          'primary-signal-hover': '#00e6af',
          'primary-signal-active': '#00cc9b',
          'primary-signal-dark': '#00bf92',
          'primary-signal-dark-hover': '#009974',
          'primary-signal-dark-active': '#007357',
          'primary-signal-darker': '#005944',
          'misty-teal': 'rgba(30, 80, 86, 0.4)', // misty_teal (alpha 40%)
          
          'text-primary': '#fbfffa',    // text/primary
          'text-secondary': '#b9cbc1',  // text/secondary
          
          'surface-low': '#181c1f',     // surface/low
          'surface-container': '#1c2023', // surface/container
          'surface-highest': '#313539', // surface/highest
          'surface-lowest': 'var(--color-surface-lowest)',
          'scrim': 'var(--color-scrim)',
          
          // 가이드에 정의된 브랜드 컬러 추가 (san_styleguide.md 반영)
          'brand-mint': '#A7F3D0',
          'brand-emerald': '#065F46',
          'accent-cyan': '#22D3EE',
        },
  
        // 3. 유기적인 형태를 위한 Spacing & Radius (globals.css 반영)
        spacing: {
          'xs': '4px',
          'sm': '8px',
          'md': '16px',
          'lg': '24px',
          'xl': '32px',
          'dashboard-gap': '24px', // 대시보드 메인 레이아웃 간격
        },
        borderRadius: {
          'leaf-sm': '8px',
          'leaf-lg': '48px', // globals.css의 --radius-leaf 값 기반
          'leaf-btn-lg': '48px', // 피그마 leaf/large 반영
          'leaf-btn-sm': '8px',  // 피그마 leaf/small 반영
        },
  
        // 4. 발광 효과 (Glowing Roots 컨셉)
        boxShadow: {
          'neon': 'var(--shadow-neon)',
          'neon-sm': 'var(--shadow-neon-sm)',
        },
  
        // 5. 기본 폰트 설정
        fontFamily: {
          sans: ['Pretendard'],
        },
      },
    },
    plugins: [],
  };
