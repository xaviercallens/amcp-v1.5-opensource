# Lightweight LLM Model Deployment Guide for AMCP v1.5

## Overview

This guide helps you deploy AMCP v1.5 with lightweight LLM models optimized for local development environments with limited resources (2-8GB RAM).

---

## 🎯 Recommended Models for Local Development

### **Ultra-Light Models (< 2GB RAM)**

#### **1. Phi3 3.8B (RECOMMENDED)** ⭐
- **Model**: `phi3:3.8b`
- **RAM**: 2.3GB
- **Quality**: Excellent
- **Use Case**: Best balance of quality and resource usage
- **Deployment**:
  ```bash
  ollama pull phi3:3.8b
  export OLLAMA_MODEL=phi3:3.8b
  ```

#### **2. Gemma 2B**
- **Model**: `gemma:2b`
- **RAM**: 1.4GB
- **Quality**: Good
- **Use Case**: Very resource-constrained environments
- **Deployment**:
  ```bash
  ollama pull gemma:2b
  export OLLAMA_MODEL=gemma:2b
  ```

#### **3. Qwen2 1.5B**
- **Model**: `qwen2:1.5b`
- **RAM**: 0.9GB
- **Quality**: Fair
- **Use Case**: Minimal resource usage
- **Deployment**:
  ```bash
  ollama pull qwen2:1.5b
  export OLLAMA_MODEL=qwen2:1.5b
  ```

---

### **Light Models (2-4GB RAM)**

#### **4. Llama 3 8B**
- **Model**: `llama3:8b`
- **RAM**: 4.7GB
- **Quality**: Very Good
- **Use Case**: Better reasoning capabilities
- **Deployment**:
  ```bash
  ollama pull llama3:8b
  export OLLAMA_MODEL=llama3:8b
  ```

#### **5. Mistral 7B**
- **Model**: `mistral:7b`
- **RAM**: 4.1GB
- **Quality**: Very Good
- **Use Case**: Strong general-purpose model
- **Deployment**:
  ```bash
  ollama pull mistral:7b
  export OLLAMA_MODEL=mistral:7b
  ```

---

## 🚀 Quick Start with Phi3 3.8B

### **Step 1: Install Ollama**

```bash
# Linux/macOS
curl -fsSL https://ollama.com/install.sh | sh

# Verify installation
ollama --version
```

### **Step 2: Pull Phi3 3.8B Model**

```bash
ollama pull phi3:3.8b
```

This will download approximately 2.3GB of data.

### **Step 3: Test the Model**

```bash
ollama run phi3:3.8b
```

Try a test prompt:
```
>>> What is the weather like in Paris?
```

Press `Ctrl+D` to exit.

### **Step 4: Configure AMCP**

```bash
# Set the model environment variable
export OLLAMA_MODEL=phi3:3.8b

# Optional: Set Ollama base URL (default is http://localhost:11434)
export OLLAMA_BASE_URL=http://localhost:11434

# Enable Ollama in AMCP
export OLLAMA_ENABLED=true
```

### **Step 5: Run AMCP CLI**

```bash
cd /home/kalxav/CascadeProjects/amcp-v1.5-opensource
mvn clean package -DskipTests
java -jar cli/target/amcp-cli-1.5.0.jar demo
```

---

## 🔧 Configuration Options

### **Environment Variables**

| Variable | Default | Description |
|----------|---------|-------------|
| `OLLAMA_MODEL` | `phi3:3.8b` | Model to use for LLM operations |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_ENABLED` | `true` | Enable/disable Ollama integration |
| `OLLAMA_TIMEOUT` | `30` | Request timeout in seconds |

### **Programmatic Configuration**

```java
// Use default Phi3 3.8B
OllamaConnectorConfig config = new OllamaConnectorConfig();

// Or specify custom model
OllamaConnectorConfig config = new OllamaConnectorConfig(
    "http://localhost:11434",  // base URL
    "phi3:3.8b",               // model name
    30,                         // timeout seconds
    3,                          // max retries
    60                          // health check interval
);
```

---

## 📊 Model Comparison

| Model | RAM | Quality | Speed | Best For |
|-------|-----|---------|-------|----------|
| **Phi3 3.8B** ⭐ | 2.3GB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Balanced quality & resources |
| Gemma 2B | 1.4GB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Resource-constrained |
| Qwen2 1.5B | 0.9GB | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Minimal resources |
| Llama 3 8B | 4.7GB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | Best quality |
| Mistral 7B | 4.1GB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | General purpose |
| TinyLlama | 0.6GB | ⭐⭐ | ⭐⭐⭐⭐⭐ | Legacy/testing |

---

## 🐛 Troubleshooting

### **Issue: "Empty response from agent"**

**Cause**: The orchestrator couldn't parse the agent response.

**Solution**: This has been fixed in the latest code. The orchestrator now checks multiple response field names:
- `response`
- `formattedResponse`
- `weatherData`

### **Issue: "OLLAMA request failed"**

**Cause**: Ollama service not running or model not available.

**Solutions**:

1. **Check if Ollama is running**:
   ```bash
   curl http://localhost:11434/api/tags
   ```

2. **Start Ollama service**:
   ```bash
   ollama serve
   ```

3. **Verify model is pulled**:
   ```bash
   ollama list
   ```

4. **Pull the model if missing**:
   ```bash
   ollama pull phi3:3.8b
   ```

### **Issue: Out of Memory**

**Cause**: Model requires more RAM than available.

**Solutions**:

1. **Check system resources**:
   ```bash
   free -h
   ```

2. **Use a lighter model**:
   ```bash
   # Switch to Gemma 2B (1.4GB)
   export OLLAMA_MODEL=gemma:2b
   ollama pull gemma:2b
   ```

3. **Close other applications** to free up RAM.

### **Issue: Slow response times**

**Cause**: CPU-bound processing on limited hardware.

**Solutions**:

1. **Use a smaller model** (Gemma 2B or Qwen2 1.5B)
2. **Reduce max_tokens** in model configuration
3. **Enable GPU acceleration** if available:
   ```bash
   # For NVIDIA GPUs
   ollama run phi3:3.8b --gpu
   ```

---

## 💡 Best Practices

### **1. Model Selection**

- **Development**: Use Phi3 3.8B for best quality/resource balance
- **Testing**: Use Gemma 2B for faster iteration
- **Production**: Use Llama 3 8B or Mistral 7B if resources allow

### **2. Resource Management**

```bash
# Monitor Ollama resource usage
ps aux | grep ollama

# Check model memory usage
ollama ps
```

### **3. Performance Optimization**

- **Keep Ollama running** in the background (don't restart for each request)
- **Use conversation context** to reduce redundant processing
- **Cache common responses** when possible
- **Set appropriate timeouts** (30s is usually sufficient)

### **4. Development Workflow**

```bash
# Terminal 1: Keep Ollama running
ollama serve

# Terminal 2: Run AMCP
cd /home/kalxav/CascadeProjects/amcp-v1.5-opensource
export OLLAMA_MODEL=phi3:3.8b
mvn clean package -DskipTests
java -jar cli/target/amcp-cli-1.5.0.jar demo
```

---

## 🔍 Testing Your Setup

### **1. Test Ollama Directly**

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "phi3:3.8b",
  "prompt": "What is the weather in Paris?",
  "stream": false
}'
```

### **2. Test AMCP Integration**

```bash
java -jar cli/target/amcp-cli-1.5.0.jar demo
```

Then try:
```
💬 You: What is the weather in Paris?
```

Expected output:
```
🔄 Processing your request...
🌤️ Current weather in Paris:
🌡️ Temperature: 12.5°C (54.5°F)
☁️ Conditions: partly cloudy
...
```

---

## 📚 Additional Resources

- **Ollama Documentation**: https://ollama.com/docs
- **Phi3 Model Card**: https://ollama.com/library/phi3
- **AMCP Documentation**: See `README.md`
- **Model Configuration**: See `ModelConfiguration.java`

---

## 🆘 Getting Help

If you encounter issues:

1. **Check logs** for error messages
2. **Verify Ollama is running**: `curl http://localhost:11434/api/tags`
3. **Check model availability**: `ollama list`
4. **Review environment variables**: `env | grep OLLAMA`
5. **Try a different model** if current one fails

---

## 📝 Summary

**For 2GB RAM systems, use Phi3 3.8B**:
```bash
ollama pull phi3:3.8b
export OLLAMA_MODEL=phi3:3.8b
java -jar cli/target/amcp-cli-1.5.0.jar demo
```

This configuration provides excellent quality with minimal resource requirements, perfect for local development and testing.
