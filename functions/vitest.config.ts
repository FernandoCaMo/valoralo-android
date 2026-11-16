import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    // Integration tests share one emulator; run files one after another.
    fileParallelism: false,
    testTimeout: 30000,
    hookTimeout: 30000,
  },
});
