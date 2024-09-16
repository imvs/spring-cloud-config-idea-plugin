# spring-cloud-config-helper

Merge Spring application profiles into a single pretty YAML configuration.

---
Plugin based on Spring Cloud Config API. I's very handy for debugging configurations with many profiles.

## Usage
<ol>
  <li>Press <samp>shift+ctrl+P</samp> and <samp>shift+ctrl+M</samp> to open plugin dialog</li>
  <li>Set the values:
    <ul>
      <li><b>Application name</b> - Spring application name (aka <samp>spring.application.name</samp>)</li>
      <li><b>Profiles</b> - Comma separated profiles names (aka as <samp>spring.profiles.active</samp>)</li>
      <li><b>Search locations</b> - Comma separated directories contained configuration files</li>
      <li><b>Output file</b> - Where to save merged configuration</li>
    </ul>
  </li>
</ol>

![img.png](img.png)