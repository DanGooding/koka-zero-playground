import type { Language, CommentTokens, InputSelection } from "prism-code-editor"
import type { Grammar } from "prism-code-editor/prism"
import { getLineBefore } from "prism-code-editor/utils"

export const kokaGrammar: Grammar = {
    comment: {
        pattern: /\/\/.*|\/\*[\s\S]*?(?:\*\/|$)/,
        greedy: true
    },
    number: /-?\b\d+(?:\.\d+)?(?:e[+-]?\d+)?\b/i,
    // repurpose 'boolean' for any constructor
    boolean: {
        pattern: /(:?[^a-z0-9_-])(?:False|True|Nil|Cons)(?=[^a-z0-9_-]|$)/,
        lookbehind: true,
    },
    keyword: {
        pattern:/(:?[^a-z0-9_-]|^)(fun|fn|with|effect|control|ctl|handler|handle|if|then|else|elif|val|match)(?=[^a-z0-9_-]|$)/,
        lookbehind: true,
    },
    builtin: {
        pattern:/(:?[^a-z0-9_-]|^)(resume)(?=[^a-z0-9_-]|$)/,
        lookbehind: true,
    },
    // types
    "class-name": {
        pattern: /(:?:\s*|^)(int|bool)(?=[^a-zA-Z0-9_-]|$)/,
        lookbehind: true,
    },
    identifier: {
        pattern: /(:?[^a-z0-9_-]|^)[a-z_][a-zA-Z0-9_-]*[a-zA-Z0-9_]?(?=[^a-z0-9_-]|$)/,
        greedy: true,
        lookbehind: true,
    },
    operator: /[+*/=!%-]|(==|!=|<=|>=|<|>|&&|\|\|)/,
    punctuation: /[(){}[\],;:]/,
}

// rules for when to add closing brackets / indent within {}
// copied from bracketIndenting in prism-code-editor since it doesn't seem to be exported
const isBracketPair = /\[]|\(\)|{}/
const openBracket = /[([{][^)\]}]*$/

const testBracketPair =
    ([start, end]: InputSelection, value: string) => {
    return isBracketPair.test(value[start - 1] + value[end])
}

const comments: CommentTokens = {
    line: "//",
    block: ["/*", "*/"],
}

export const kokaLanguage: Language = ({
    comments,
    autoIndent: [
        ([start], value) => openBracket.test(getLineBefore(value, start)),
        testBracketPair,
    ],
})