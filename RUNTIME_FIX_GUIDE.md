# 🔧 AMCP CLI - Runtime Dependency Fix Guide

## ❌ **Problem: ClassNotFoundException for Jackson**

### **Error Message:**
```
Exception in thread "main" java.lang.NoClassDefFoundError: com/fasterxml/jackson/databind/ObjectMapper
Caused by: java.lang.ClassNotFoundException: com.fasterxml.jackson.databind.ObjectMapper
```

### **Root Cause:**
The error occurs when running the CLI JAR without its dependencies. The `original-amcp-cli-1.5.0.jar` (57KB) contains only the compiled CLI code, not the required libraries.

---

## ✅ **Solution: Use the Shaded JAR**

### **Option 1: Use the Run Script (Recommended)**

```bash
# From the amcp-v1.5-opensource directory
./run-amcp-cli.sh
```

This script automatically:
- Checks if the JAR is built
- Builds if necessary
- Runs the correct shaded JAR with all dependencies

### **Option 2: Run the Shaded JAR Directly**

```bash
# Use the shaded JAR (16MB - includes all dependencies)
java -jar cli/target/amcp-cli-1.5.0.jar

# OR use the jar-with-dependencies version
java -jar cli/target/amcp-cli-1.5.0-jar-with-dependencies.jar
```

### **Option 3: Rebuild and Run**

```bash
# Build the project
mvn clean package -DskipTests

# Run the shaded JAR
java -jar cli/target/amcp-cli-1.5.0.jar
```

---

## 📦 **Understanding the JAR Files**

| JAR File | Size | Description | Can Run? |
|----------|------|-------------|----------|
| `original-amcp-cli-1.5.0.jar` | 57KB | CLI code only | ❌ No - Missing dependencies |
| `amcp-cli-1.5.0.jar` | 16MB | Shaded JAR with all deps | ✅ Yes - Complete |
| `amcp-cli-1.5.0-jar-with-dependencies.jar` | 16MB | Assembly JAR with all deps | ✅ Yes - Complete |

---

## 🔍 **How the Fix Works**

### **Maven Shade Plugin Configuration:**

The `cli/pom.xml` includes the Maven Shade plugin which creates an "uber JAR" containing:
- All CLI code
- All AMCP dependencies (core, connectors, examples)
- All third-party libraries (Jackson, JLine, etc.)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.4.1</version>
    <configuration>
        <finalName>amcp-cli-${project.version}</finalName>
        <transformers>
            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                <mainClass>io.amcp.cli.AMCPInteractiveCLI</mainClass>
            </transformer>
        </transformers>
    </configuration>
</plugin>
```

---

## ✅ **Verification**

### **Test the CLI:**

```bash
# Run the CLI
./run-amcp-cli.sh

# You should see:
🚀 Starting AMCP Interactive CLI v1.5.0
========================================

🎮 Launching AMCP CLI...

╔═══════════════════════════════════════════════════════════╗
║        AMCP Interactive CLI - v1.5.0-OPENSOURCE           ║
║     Agent Mesh Communication Protocol                     ║
╚═══════════════════════════════════════════════════════════╝

# Test activating the orchestrator
amcp> activate orchestrator
```

### **Expected Result:**
✅ The orchestrator should activate without ClassNotFoundException

---

## 🚀 **Quick Start Commands**

### **Build and Run:**
```bash
# 1. Build the project
cd amcp-v1.5-opensource
mvn clean package -DskipTests

# 2. Run the CLI
./run-amcp-cli.sh

# 3. Test orchestrator activation
amcp> activate orchestrator
amcp> status
```

### **Available Agents:**
```
- orchestrator  : LLM-powered orchestration agent
- eventbroker   : Event routing and management
- openweather   : Weather data integration
- polygon.io    : Financial data integration
```

---

## 📝 **Additional Notes**

### **Why Two JAR Files?**

1. **Maven Shade Plugin** creates `amcp-cli-1.5.0.jar`
   - Relocates conflicting dependencies
   - Merges service files
   - Optimized for distribution

2. **Maven Assembly Plugin** creates `amcp-cli-1.5.0-jar-with-dependencies.jar`
   - Simple concatenation of all JARs
   - Backup option if shade has issues

Both work, but the shaded JAR is preferred.

### **Development Mode:**

If you're developing and want to run from IDE:
- Ensure all dependencies are on the classpath
- Or use `mvn exec:java -Dexec.mainClass="io.amcp.cli.AMCPInteractiveCLI"`

---

## ✅ **Status: FIXED**

The runtime dependency issue is resolved by using the correct JAR file. The `run-amcp-cli.sh` script ensures you always use the right JAR with all dependencies included.

**Next Steps:**
1. Use `./run-amcp-cli.sh` to start the CLI
2. Activate agents with `activate <agent-name>`
3. Enjoy the full AMCP experience!

---

**Script Location**: `./run-amcp-cli.sh`  
**Shaded JAR**: `cli/target/amcp-cli-1.5.0.jar` (16MB)  
**Status**: ✅ Ready to use
