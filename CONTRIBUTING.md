# Contributing to Spring Boot Distributed Rate Limiter

Thank you for your interest in contributing! This project welcomes all contributions.

---

## Ways to Contribute

- 🐛 Report bugs via [GitHub Issues](https://github.com/tarunve/spring-boot-custom-rate-limiter/issues)
- 💡 Suggest new features
- 📖 Improve documentation
- 🔧 Submit pull requests

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (for Redis integration tests)

### Build

```bash
git clone https://github.com/tarunve/spring-boot-custom-rate-limiter.git
cd spring-boot-custom-rate-limiter
mvn clean install
```

### Run the demo

```bash
cd rate-limiter-demo
mvn spring-boot:run
# Try it:
curl http://localhost:8080/api/public/hello
```

---

## Pull Request Process

1. Fork the repo and create a feature branch: `git checkout -b feature/my-feature`
2. Write tests for your changes
3. Ensure `mvn clean verify` passes
4. Submit a PR with a clear description

---

## Code Style

- Follow existing code conventions
- Use Lombok to reduce boilerplate
- Add Javadoc to public APIs
- Keep methods focused and small

---

## Ideas for Contributions

| Area | Description | Difficulty |
|---|---|---|
| Sliding window algorithm | Alternative to token bucket | Medium |
| Micrometer metrics | Expose rate limit metrics | Easy |
| JWT claim key resolver | Rate limit by JWT subject | Easy |
| Header-based key resolver | Rate limit by custom header | Easy |
| Rate limit events | Application events on limit exceeded | Easy |
| Redis Cluster support | Multi-node Redis | Hard |
| Rate limit admin UI | Web UI to view/manage limits | Hard |

---

## License

By contributing, you agree your contributions will be licensed under the MIT License.
