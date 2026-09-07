export interface ArchiveCategoryResponse {
  categoryId: string;
  categoryName: string;
  cardCount: number;
}

export interface ArchiveCategoryListResponse {
  categories: ArchiveCategoryResponse[];
}

export interface ArchiveCardTagResponse {
  tagId: string;
  tagName: string;
}

export interface ArchiveCategoryCardResponse {
  cardId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  tags: ArchiveCardTagResponse[];
}

export interface ArchiveCategoryCardListResponse {
  categoryId: string;
  categoryName: string;
  cards: ArchiveCategoryCardResponse[];
}

export interface ArchiveRelatedCardResponse {
  cardId: string;
  categoryId: string;
  categoryName: string;
  title: string;
  matchedTagCount: number;
  matchedTags: ArchiveCardTagResponse[];
}

export interface ArchiveCardTagRelationResponse {
  selectedCardId: string;
  relatedCards: ArchiveRelatedCardResponse[];
}
