import { create } from 'zustand';
import { type KnowledgeCardView } from '@san/shared';

interface ScrapStore {
  cards: KnowledgeCardView[];
  viewMode: 'card' | 'graph';
  setCards: (cards: KnowledgeCardView[]) => void;
  setViewMode: (mode: 'card' | 'graph') => void;
}

export const useScrapStore = create<ScrapStore>((set) => ({
  cards: [],
  viewMode: 'card',
  setCards: (cards) => set({ cards }),
  setViewMode: (viewMode) => set({ viewMode }),
}));