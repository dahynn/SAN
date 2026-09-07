import type { GraphCategory, GraphLeaf } from '../../types/graph';

export const graphFixtureCategories: Omit<GraphCategory, 'position' | 'sphere'>[] = [
  { id: 'nature', name: '자연' },
  { id: 'science', name: '과학' },
  { id: 'history', name: '역사' },
  { id: 'art', name: '예술' },
  { id: 'philosophy', name: '철학' },
];

export const graphFixtureLeavesByCategory: Record<string, Omit<GraphLeaf, 'position'>[]> = {
  nature: [
    {
      id: 'cycle',
      title: '자연의 순환',
      summary: '모든 생명은 서로 연결되어 순환한다.',
      tags: ['자연', '순환'],
      collectedAt: '2024.05.31',
    },
    {
      id: 'water',
      title: '물의 지혜',
      summary: '물은 가장 낮은 곳으로 흐르며 생태계를 살린다.',
      tags: ['자연', '생태'],
      collectedAt: '2024.05.24',
    },
    {
      id: 'light',
      title: '빛의 언어',
      summary: '빛은 보이지 않던 결을 드러내는 가장 조용한 신호다.',
      tags: ['자연', '감각'],
      collectedAt: '2024.05.19',
    },
    {
      id: 'wind',
      title: '바람의 기억',
      summary: '방향은 사라져도 흔적은 풍경 위에 남는다.',
      tags: ['감각', '기억'],
      collectedAt: '2024.05.12',
    },
    {
      id: 'time',
      title: '시간의 흐름',
      summary: '시간은 겹겹의 결을 만들고 풍경을 성숙시킨다.',
      tags: ['기억', '순환'],
      collectedAt: '2024.05.03',
    },
    {
      id: 'seed',
      title: '씨앗의 잠',
      summary: '성장은 멈춘 것이 아니라 준비되는 시간이다.',
      tags: ['성장', '계절'],
      collectedAt: '2024.04.27',
    },
    {
      id: 'forest',
      title: '숲의 협업',
      summary: '숲은 경쟁보다 연결로 오래 살아남는다.',
      tags: ['생태', '연결'],
      collectedAt: '2024.04.19',
    },
    {
      id: 'moss',
      title: '이끼의 인내',
      summary: '작은 생명은 가장 오래된 표면을 천천히 바꾼다.',
      tags: ['생명', '시간'],
      collectedAt: '2024.04.10',
    },
    {
      id: 'river',
      title: '강의 선택',
      summary: '강은 가장 쉬운 길이 아니라 가장 오래 이어질 길을 찾는다.',
      tags: ['물', '지형'],
      collectedAt: '2024.04.02',
    },
    {
      id: 'season',
      title: '계절의 문장',
      summary: '같은 장소도 계절마다 다른 의미를 쓴다.',
      tags: ['계절', '변화'],
      collectedAt: '2024.03.25',
    },
  ],
};
