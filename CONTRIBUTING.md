# Contributing to EzShops

Thank you for your interest in contributing to EzShops! We welcome contributions from everyone. Please read these guidelines to help us maintain a high-quality, collaborative project.

## How to Contribute

### 1. Reporting Issues
- Use [GitHub Issues](https://github.com/ez-plugins/EzShops/issues) to report bugs, request features, or ask questions.
- Please provide as much detail as possible, including steps to reproduce, logs, and your environment.
- Include EzShops version, server version (Spigot/Paper), and Java version.

### 2. Submitting Pull Requests
- Fork the repository and create your branch from `main`.
- Follow the existing code style and conventions.
- Write clear, concise commit messages.
- Include tests for new features or bug fixes when possible.
- Ensure your code builds and passes all tests before submitting.
- Reference related issues in your PR description (e.g., `Closes #123`).

### 3. Code Style
- Use 4 spaces for indentation (Java).
- Use descriptive names for classes, methods, and variables.
- Document public classes and methods with Javadoc.
- Keep gameplay features organized in dedicated packages or classes—such as commands, GUIs, and managers—rather than expanding `EzShopsPlugin` with additional logic. Follow the existing package structure under `src/main/java/com/skyblockexp` when adding new functionality.

### 4. Documentation
- Update documentation in the `docs/` folder as needed.
- Add or update code samples and API references if your change affects public APIs.
- Keep the README.md in sync with significant feature changes.

### 5. Testing
- Test your changes thoroughly on a local server before submitting.
- Include unit tests for business logic when applicable.
- Ensure existing tests pass: `mvn test`
- Build the plugin successfully: `mvn clean package`

### 6. Reviewing and Merging
- All PRs are reviewed by maintainers.
- Address review comments promptly.
- PRs may be squashed and merged for a clean history.

## Development Setup

### Requirements
- Java 17 or higher
- Maven 3.6+
- A test server (Paper/Spigot 1.17+)
- Vault and an economy plugin for testing

### Building from Source
```bash
cd EzShops
mvn clean package
```

The compiled JAR will be in `target/EzShops-<version>.jar`.

### Running Tests
```bash
mvn test
```

## Project Structure

```
ezshops/
├── src/main/java/com/skyblockexp/
│   ├── ezshops/           # Main plugin code and bootstrap
│   ├── shop/              # Core shop functionality
│   │   ├── api/           # Public API interfaces
│   │   ├── command/       # Command handlers
│   │   └── sign/          # Sign shop features
│   └── playershop/        # Player-owned shop system
├── src/main/resources/    # Plugin resources and configs
├── docs/                  # Documentation (this folder)
├── pom.xml               # Maven configuration
└── README.md             # Main readme
```

## Contribution Guidelines

### Feature Requests
- Open an issue to discuss the feature before implementing.
- Explain the use case and how it benefits server owners.
- Consider backward compatibility with existing configurations.

### Bug Fixes
- Reference the issue number in your PR.
- Include steps to reproduce the bug.
- Add regression tests if applicable.

### API Changes
- API changes must maintain backward compatibility when possible.
- Document breaking changes clearly in the PR description.
- Update API documentation in `docs/api.md`.

### Versioning
- Only bump Maven module versions for the projects you actually change.
- When a change affects just the EzShops plugin, update `ezshops/pom.xml` and leave the other module versions untouched.
- The parent `pom.xml` should only have its `<version>` incremented when a change spans multiple modules or shared build configuration.

## Code of Conduct

This project follows the [Contributor Covenant](https://www.contributor-covenant.org/version/2/1/code_of_conduct/). Be respectful and inclusive in all interactions.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

## Getting Help

- **Questions?** Open a discussion on GitHub or join our [Discord server](https://discord.gg/yWP95XfmBS)
- **Found a bug?** Open an issue with detailed reproduction steps
- **Want to contribute but don't know where to start?** Look for issues labeled `good first issue` or `help wanted`

Thank you for helping make EzShops better!
