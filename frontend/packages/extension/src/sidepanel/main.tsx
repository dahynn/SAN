import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { initTheme } from '@san/ui';
import SidePanel from './SidePanel';
import '@san/ui/styles/globals.css';
import './sidepanel.css';

initTheme();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SidePanel />
  </StrictMode>,
);
