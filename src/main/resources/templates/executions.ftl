<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    </meta>
    <title>Log analysis results as of ${timestamp}</title>
  </head>
  <body style="font-size: 12px">
    <h1>Recent log analysis results</h1>
    <table border="1" style="width: 100%; text-align:center;">
      <tr>
        <th>Version</th>
        <th>Start</th>
        <th>End</th>
        <th>Duration</th>
        <th>Clean?</th>
        <th>Commit</th>
        <th>Lines</th>
        <th>Records</th>
        <th>Error</th>
        <th>Warn</th>
        <th>Info</th>
        <#if showOthers><th>Other</th></#if>
        <th>Timer</th>
        <th>Min</th>
        <th>Max</th>
        <th>Sum</th>
        <th>Average</th>
        <th>Median</th>
        <th>Satisfied</th>
        <th>Tolerant</th>
        <th>Frustrated</th>
        <th>Apdex</th>
      </tr>
      <#list 0..executions?size-1 as i>
      <#assign execution = executions[i]>
      <tr style=${(i == 0) ? string("font-weight:bold;background:#43C480;","")}>
        <td>${execution.odeeVersion}</td>
        <td>${execution.startTimestamp}</td>
        <td>${execution.endTimestamp}</td>
        <td>${execution.getLogDurationFormatted()}</td>
        <td>${execution.clean?string}</td>
        <td>
          <a href="https://github.com/ModiusOpenData/modius-qa/commit/${execution.commit}" target="_blank" rel="noopener noreferrer">${execution.commit?substring(0,6)}</a>
        </td>
        <td>${execution.logLinesTotalNumber}</td>
        <td>${execution.logRecordsNumber}</td>
        <td>${execution.errorRecordsNumber}</td>
        <td>${execution.warnRecordsNumber}</td>
        <td>${execution.infoRecordsNumber}</td>
        <#if showOthers><td>${execution.getOtherLevelRecordsNumber()}</td></#if>
        <td>${execution.springTimerFilterRecordsNumber}</td>
        <td>${execution.minResponseDuration}</td>
        <td>${execution.maxResponseDuration}</td>
        <td>${execution.sumResponseDuration}</td>
        <td>${execution.averageResponseDuration}</td>
        <td>${execution.medianResponseDuration}</td>
        <td>${execution.satisfiedCount}</td>
        <td>${execution.tolerantCount}</td>
        <td>${execution.getFrustratedCount()}</td>
        <td>${execution.getApdex()}</td>
      </tr>
      </#list>
    </table>
    <h1>${lastExecution.odeeVersion} top slowest responses</h1>
    <table border="1" style="width: 100%; text-align:center;">
        <tr>
            <th>Method</th>
            <th>Path</th>
            <th>Duration</th>
        </tr>
    <#list topTenSlowestResponses as slowRessponse>
        <tr style="${(slowRessponse.duration > 5000) ? string("background:#EC4554;","")}">
            <td>${slowRessponse.method}</td>
            <td>${slowRessponse.urlPath}</td>
            <td>${slowRessponse.duration}</td>
        </tr>
    </#list>
    </table>
  </body>
</html>