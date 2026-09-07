export const hangulAdjacentStrongPlugin = () => (tree: MarkdownNode) => {
  visitMarkdownNode(tree);
};

const HANGUL_LETTER_PATTERN = /[\u3131-\u318E\uAC00-\uD7A3]/;
const HANGUL_ADJACENT_STRONG_PATTERN = /\*\*([^*\n]+?)\*\*(?=[\u3131-\u318E\uAC00-\uD7A3])/g;
const MARKDOWN_FLANKING_BREAK = '&#8203;';

interface MarkdownNode {
  type?: string;
  value?: string;
  children?: MarkdownNode[];
}

export function normalizeHangulAdjacentStrong(content: string) {
  return content.replace(HANGUL_ADJACENT_STRONG_PATTERN, `**$1**${MARKDOWN_FLANKING_BREAK}`);
}

function visitMarkdownNode(node: MarkdownNode) {
  if (!node.children) return;

  for (let index = 0; index < node.children.length; index += 1) {
    const child = node.children[index];

    if (child.type === 'text' && child.value?.includes('**') && HANGUL_LETTER_PATTERN.test(child.value)) {
      const replacement = splitHangulAdjacentStrongText(child.value);
      if (replacement.length > 1) {
        node.children.splice(index, 1, ...replacement);
        index += replacement.length - 1;
        continue;
      }
    }

    visitMarkdownNode(child);
  }
}

function splitHangulAdjacentStrongText(value: string): MarkdownNode[] {
  const nodes: MarkdownNode[] = [];
  let cursor = 0;

  value.replace(HANGUL_ADJACENT_STRONG_PATTERN, (match, strongText: string, offset: number) => {
    if (offset > cursor) {
      nodes.push({ type: 'text', value: value.slice(cursor, offset) });
    }

    nodes.push({
      type: 'strong',
      children: [{ type: 'text', value: strongText }],
    });

    cursor = offset + match.length;
    return match;
  });

  if (nodes.length === 0) return [{ type: 'text', value }];
  if (cursor < value.length) {
    nodes.push({ type: 'text', value: value.slice(cursor) });
  }

  return nodes;
}
