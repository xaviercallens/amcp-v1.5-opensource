# Qwen2.5:0.5b Local Development Configuration - COMPLETE ✅

## 🎉 Configuration Status: READY FOR TESTING

Your local development environment is now fully configured with **Qwen2.5:0.5b** and comprehensive testing capabilities!

## 📊 **Installed Models**

| Model | Size | RAM Usage | Status | Use Case |
|-------|------|-----------|--------|----------|
| **🆕 qwen2.5:0.5b** | **397 MB** | **0.4GB** | ✅ **READY** | **Ultra-minimal, ultra-fast** |
| gemma:2b | 1.7 GB | 1.4GB | ✅ READY | Recommended: Fast & efficient |
| qwen2:1.5b | 934 MB | 0.9GB | ✅ READY | Ultra-lightweight |
| qwen2:7b | 4.4 GB | 4.4GB | ✅ READY | Large context, multilingual |
| phi3:3.8b | 2.2 GB | 2.3GB | ✅ READY | Excellent quality |

## 🔧 **Environment Configuration**

### **Environment Variables** (Auto-configured in `~/.amcp-env`)
```bash
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen2.5:0.5b          # Default to ultra-minimal model
export AMCP_GPU_LAYERS=auto
export AMCP_GPU_VRAM_MB=auto
export AMCP_LLM_TIMEOUT=120
export AMCP_CACHE_ENABLED=true
export AMCP_CACHE_TTL_HOURS=24
```

### **Load Environment**
```bash
source ~/.amcp-env
```

## 🧪 **Testing Commands**

### **1. Quick Model Tests**
```bash
# Test Qwen2.5:0.5b (ultra-minimal)
echo "What is artificial intelligence?" | ollama run qwen2.5:0.5b

# Test Gemma 2B (recommended)
echo "Explain machine learning" | ollama run gemma:2b

# Test Qwen2 7B (large context)
echo "Write a short story" | ollama run qwen2:7b
```

### **2. Interactive Chat**
```bash
# Start interactive session with Qwen2.5:0.5b
ollama run qwen2.5:0.5b

# Commands in interactive mode:
# - Type your questions directly
# - /bye to exit
```

### **3. AMCP Integration Tests**
```bash
# Quick automated test
~/test-amcp-models.sh

# Comprehensive interactive tester
cd /home/kalxav/CascadeProjects/amcp-v1.5-opensource/connectors
mvn exec:java -Dexec.mainClass="io.amcp.connectors.ai.terminal.LocalModelTester"
```

### **4. Model Management**
```bash
# List all installed models
ollama list

# Check model info
ollama show qwen2.5:0.5b

# Remove a model (if needed)
ollama rm tinyllama:latest
```

## 🎯 **AMCP LocalModelTester Menu**

When you run the interactive tester, you'll see:

```
╔══════════════════════════════════════════════════════════════╗
║                    AMCP Model Tester v1.5.1                 ║
║              Local LLM Testing & Configuration              ║
╚══════════════════════════════════════════════════════════════╝

Available Options:
1. Test Recommended Model
2. Test Specific Model
3. Compare Multiple Models
4. Benchmark Model Performance
5. Check Ollama Status
6. Show Model Information
7. Test NEW Qwen2.5:0.5b Model ⭐
q. Quit
```

## 📈 **Performance Characteristics**

### **Qwen2.5:0.5b (NEW Ultra-Minimal)**
- **RAM**: Only 0.4GB
- **Speed**: Ultra-fast responses
- **Timeout**: 60 seconds (optimized)
- **Best for**: Edge devices, containers, quick testing
- **Temperature**: 0.5 (consistent responses)
- **Context**: 8K tokens

### **Gemma 2B (Recommended)**
- **RAM**: 1.4GB
- **Speed**: Fast and efficient
- **Timeout**: 90 seconds
- **Best for**: General development, balanced performance
- **Temperature**: 0.6 (focused responses)
- **Context**: 8K tokens

### **Qwen2 7B (Large Context)**
- **RAM**: 4.4GB
- **Speed**: Slower but high quality
- **Timeout**: 120 seconds
- **Best for**: Complex tasks, multilingual
- **Temperature**: 0.7 (creative responses)
- **Context**: 32K tokens

## 🔍 **Verification Tests**

### **Test 1: Basic Functionality**
```bash
echo "Hello, test Qwen2.5:0.5b" | ollama run qwen2.5:0.5b
# Expected: Quick response about the greeting
```

### **Test 2: AMCP Integration**
```bash
cd connectors
mvn test -Dtest=ModelConfigurationTest -q
# Expected: All tests pass
```

### **Test 3: Performance Comparison**
```bash
# Time different models
time echo "What is AI?" | ollama run qwen2.5:0.5b
time echo "What is AI?" | ollama run gemma:2b
# Expected: Qwen2.5:0.5b should be fastest
```

## 🚀 **Development Workflow**

### **1. Daily Development**
```bash
# Load environment
source ~/.amcp-env

# Quick test
echo "Test prompt" | ollama run $OLLAMA_MODEL

# Development with AMCP
cd /home/kalxav/CascadeProjects/amcp-v1.5-opensource/connectors
mvn exec:java -Dexec.mainClass="io.amcp.connectors.ai.terminal.LocalModelTester"
```

### **2. Model Selection Guide**
- **Quick testing**: Use `qwen2.5:0.5b` (ultra-fast)
- **Development**: Use `gemma:2b` (balanced)
- **Complex tasks**: Use `qwen2:7b` (high quality)
- **Resource-constrained**: Use `qwen2.5:0.5b` (minimal)

### **3. Performance Monitoring**
```bash
# Check system resources
htop

# Monitor Ollama
ollama ps

# AMCP statistics available in LocalModelTester
```

## 🛠️ **Troubleshooting**

### **Common Issues & Solutions**

#### **Model Not Responding**
```bash
# Check Ollama service
ollama list
ollama ps

# Restart if needed
pkill ollama
ollama serve &
```

#### **Timeout Issues**
```bash
# For slow responses, use larger models
ollama run gemma:2b  # Instead of qwen2.5:0.5b

# Or increase timeout in AMCP configuration
export AMCP_LLM_TIMEOUT=180
```

#### **Memory Issues**
```bash
# Use ultra-minimal model
ollama run qwen2.5:0.5b

# Check memory usage
free -h
```

## 📋 **Configuration Files**

### **Environment**: `~/.amcp-env`
- AMCP-specific environment variables
- Model defaults and timeouts
- Cache configuration

### **Test Script**: `~/test-amcp-models.sh`
- Quick validation script
- Tests Qwen2.5:0.5b functionality
- Provides usage instructions

### **AMCP Integration**: `/home/kalxav/CascadeProjects/amcp-v1.5-opensource/`
- Full LocalModelTester utility
- Comprehensive testing framework
- Model configuration and optimization

## 🎊 **Ready for Development!**

Your local development environment is now configured with:

✅ **Qwen2.5:0.5b** - Ultra-minimal model (0.4GB RAM)  
✅ **Multiple model options** - From ultra-minimal to high-performance  
✅ **AMCP integration** - Full orchestration capabilities  
✅ **Interactive testing** - LocalModelTester utility  
✅ **Automated setup** - Environment and scripts configured  
✅ **Performance optimization** - Model-specific timeouts and parameters  

**Start testing with**: `echo "Hello Qwen2.5!" | ollama run qwen2.5:0.5b`

**Happy coding!** 🚀
