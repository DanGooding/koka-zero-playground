import './style.css'
import {setupCounter} from './counter.ts'
import sourceCode from './fibonacci-generator.kk?raw'

document.querySelector<HTMLTextAreaElement>('#source-code')!.textContent = sourceCode

setupCounter(document.querySelector<HTMLButtonElement>('#counter')!)
