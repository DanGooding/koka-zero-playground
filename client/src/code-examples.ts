import type { CodeExample } from "./state.ts";
import fibonacciGeneratorCode from './fibonacci-generator.kk?raw'
import pythagoreanSearchCode from './pythagorean-search.kk?raw'
import coroutinesCode from './coroutines.kk?raw'

export function codeExampleToButtonId(codeExample: CodeExample): string {
    return codeExample;
}

export function codeExampleOfButtonId(id: string): CodeExample {
    if (id === "fibonacciGenerator" || id === "pythagoreanSearch" || id === "coroutines" || id === "blank") {
        return id
    }else {
        console.error('unexpected code example id', id)
        return "blank"
    }
}

export function codeExampleCode(codeExample: CodeExample): string {
    switch (codeExample) {
        case "fibonacciGenerator":
            return fibonacciGeneratorCode
        case "pythagoreanSearch":
            return pythagoreanSearchCode
        case "coroutines":
          return coroutinesCode
        case "blank":
            return "\n"
    }
}
