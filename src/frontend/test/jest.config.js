module.exports = {
    clearMocks: true,
    coverageDirectory: "<rootDir>/../../../target/frontend-coverage",
    coveragePathIgnorePatterns: [
        "/node_modules/"
    ],
    moduleFileExtensions: [
        "js",
        "jsx",
    ],
    roots: [
        "<rootDir>",
        "<rootDir>/../src/"
    ],
    snapshotSerializers: [],
    testEnvironment: 'jsdom',
    testMatch: [
        "**/?(*.)test.jsx"
    ],
    transform: {
        "\\.jsx?$": ["babel-jest", { configFile: './babel.config.js' }]
    }
};