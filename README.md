# log-utils
Apache Log4j processing utilities

Application arguments, in order:

```text
<logFilePath> <clean> <commit> <userName> <password> <sender> <personal> <recipients> [version]
```

The optional ninth argument supplies the OpenData version and skips version detection
from the log. When omitted or blank, the version is read from an
`Opendata [version=...]` log message.
