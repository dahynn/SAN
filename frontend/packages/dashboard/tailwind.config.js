// /packages/dashboard/tailwind.config.js
const baseConfig = require('../../tailwind.config.base.js');
const tilDraftTokens = require('./src/til-drafts.tokens.json');

const globalTokens = tilDraftTokens.global.global;
const htmlTokens = tilDraftTokens['html.to.design'];
const px = (value) => `${value}px`;

const colorTokens = globalTokens.color;
const leafTokens = globalTokens.borderradius.leaf;
const letterSpacingTokens = htmlTokens['letter spacing'];
const lineHeightTokens = htmlTokens['line height'];

module.exports = {
  ...baseConfig,
  content: ['./src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      ...baseConfig.theme.extend,
      colors: {
        ...baseConfig.theme.extend.colors,
        background: colorTokens.background.value,
        'primary-signal': colorTokens.primary_signal.value,
        'misty-teal': colorTokens.misty_teal.value,
        text: {
          primary: colorTokens.text.primary.value,
          secondary: colorTokens.text.secondary.value,
        },
        surface: {
          base: colorTokens.surface.base.value,
          low: colorTokens.surface.low.value,
          container: colorTokens.surface.container.value,
          highest: colorTokens.surface.highest.value,
        },
      },
      borderRadius: {
        ...baseConfig.theme.extend.borderRadius,
        leaf: `${px(leafTokens.large.value)} ${px(leafTokens.small.value)} ${px(leafTokens.large.value)} ${px(leafTokens.small.value)}`,
        'leaf-reverse': `${px(leafTokens.small.value)} ${px(leafTokens.large.value)} ${px(leafTokens.small.value)} ${px(leafTokens.large.value)}`,
        'leaf-soft': `${px(leafTokens.large.value)} ${px(leafTokens.large.value)} ${px(leafTokens.small.value)} ${px(leafTokens.small.value)}`,
      },
      letterSpacing: {
        ...baseConfig.theme.extend.letterSpacing,
        'token-4': px(letterSpacingTokens['4'].value),
        'token-6': px(letterSpacingTokens['6'].value),
        'token-0_6': px(letterSpacingTokens['0_6'].value),
        'token-1_2': px(letterSpacingTokens['1_2'].value),
        'token--0_5': px(letterSpacingTokens['-0_5'].value),
        'token--0_6': px(letterSpacingTokens['-0_6'].value),
        'token--1_5': px(letterSpacingTokens['-1_5'].value),
        'token--2_4': px(letterSpacingTokens['-2_4'].value),
      },
      lineHeight: {
        ...baseConfig.theme.extend.lineHeight,
        'token-16': px(lineHeightTokens['16'].value),
        'token-19_6': px(lineHeightTokens['19_6'].value),
        'token-20': px(lineHeightTokens['20'].value),
        'token-21': px(lineHeightTokens['21'].value),
        'token-24': px(lineHeightTokens['24'].value),
        'token-25_2': px(lineHeightTokens['25_2'].value),
        'token-28': px(lineHeightTokens['28'].value),
        'token-32': px(lineHeightTokens['32'].value),
        'token-32_5': px(lineHeightTokens['32_5'].value),
        'token-35_2': px(lineHeightTokens['35_2'].value),
        'token-57_6': px(lineHeightTokens['57_6'].value),
        'token-61_6': px(lineHeightTokens['61_6'].value),
        'token-132': px(lineHeightTokens['132'].value),
      },
      fontSize: {
        ...baseConfig.theme.extend.fontSize,
        'til-caption': ['10px', { lineHeight: px(lineHeightTokens['16'].value), letterSpacing: px(letterSpacingTokens['1_2'].value), fontWeight: '700' }],
        'til-body': ['16px', { lineHeight: px(lineHeightTokens['24'].value), letterSpacing: '0px', fontWeight: '400' }],
        'til-body-bold': ['16px', { lineHeight: px(lineHeightTokens['24'].value), letterSpacing: '0px', fontWeight: '700' }],
        'til-title': ['24px', { lineHeight: px(lineHeightTokens['32'].value), letterSpacing: px(letterSpacingTokens['-0_5'].value), fontWeight: '700' }],
      },
      spacing: {
        ...baseConfig.theme.extend.spacing,
        'dashboard-gap': '24px',
      },
    },
  },
};
