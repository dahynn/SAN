const baseConfig = require('../../tailwind.config.base.js');

module.exports = {
  ...baseConfig,
  content: ["./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      ...baseConfig.theme.extend,
      spacing: {
        'popover-padding': '12px', // 익스텐션 팝업용 좁은 간격
      }
    }
  }
}