export type GraphCategory = {
  id: string;
  name: string;
  position: {
    top: string;
    left: string;
  };
  sphere: {
    latitude: number;
    longitude: number;
  };
};

export type GraphLeaf = {
  id: string;
  title: string;
  summary?: string;
  tags: string[];
  collectedAt: string;
  position: {
    top: string;
    left: string;
  };
};

export type GraphRelation = {
  fromCardId: string;
  toCardId: string;
  sharedTags: string[];
};
