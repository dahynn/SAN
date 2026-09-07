const CHROME_WEB_STORE_BASE_URL = 'https://chromewebstore.google.com/detail';

export function getExtensionInstallUrl() {
  const configuredUrl = import.meta.env.VITE_SAN_EXTENSION_INSTALL_URL;
  if (configuredUrl) {
    return configuredUrl;
  }

  const extensionId = import.meta.env.VITE_SAN_EXTENSION_ID;
  return extensionId ? `${CHROME_WEB_STORE_BASE_URL}/${extensionId}` : null;
}
