import {ConfigEnv, defineConfig, UserConfig} from "vite";

export default defineConfig((configEnv: ConfigEnv): UserConfig => {
    if (configEnv.command === 'build') {
        // production
        return {} // use default

    } else if (configEnv.command === 'serve') {
        // development
        return {
            server: {
                // proxy connections to backend,
                // so that same-origin check passes
                proxy: {
                    '/ws': {
                        target: 'http://localhost:80',
                        changeOrigin: true,
                        secure: false,
                        ws: true,
                        rewriteWsOrigin: true,
                    }
                }
            },
            optimizeDeps: {exclude: ["fsevents"]},
        }
    }
})