import type {Grammar} from "prism-code-editor/prism"

export const kokaGrammar: Grammar = {
    comment: {
        pattern: /\/\/.*|\/\*[\s\S]*?(?:\*\/|$)/,
        greedy: true
    },
    number: /-?\b\d+(?:\.\d+)?(?:e[+-]?\d+)?\b/i,
    boolean: {
        pattern: /(:?[^a-z0-9_-])(?:False|True)(?![a-z0-9_-])/,
        lookbehind: true,
    },
    keyword: {
        pattern:/(:?[^a-z0-9_-])(fun|fn|with|effect|control|ctl|handler|handle|if|then|else|elif|val)(?![a-z0-9_-])/,
        lookbehind: true,
    },
    builtin: {
        pattern:/(:?[^a-z0-9_-])(resume)(?![a-z0-9_-])/,
        lookbehind: true,
    },
    // types
    "class-name": {
        pattern: /(:?:\s*)(int|bool)(?![a-zA-Z0-9_-])/,
        lookbehind: true,
    },
    identifier: {
        pattern: /(:?[^a-z0-9_-])[a-z_][a-zA-Z0-9_-]*[a-zA-Z0-9_]?(?![a-z0-9_-])/,
        greedy: true,
        lookbehind: true,
    },
    operator: /[+*/=!%-]|(==|!=|<=|>=|<|>|&&|\|\|)/,
    punctuation: /[(){},;:]/,
}