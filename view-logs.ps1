# 智能农业系统日志查看工具
# 使用方法：.\view-logs.ps1

param(
    [string]$Action = "tail",  # tail, search, ai, error, all
    [string]$Keyword = "",     # 搜索关键词
    [int]$Lines = 50          # 显示行数
)

$LogFile = "logs/smart-agriculture.log"

Write-Host @"
╔══════════════════════════════════════════╗
║        智能农业系统 - 日志查看工具        ║
╚══════════════════════════════════════════╝
"@ -ForegroundColor Cyan

# 检查日志文件是否存在
if (-not (Test-Path $LogFile)) {
    Write-Host "❌ 日志文件不存在: $LogFile" -ForegroundColor Red
    Write-Host "请先启动后端服务" -ForegroundColor Yellow
    exit 1
}

switch ($Action) {
    "tail" {
        Write-Host "📄 显示最新 $Lines 行日志:" -ForegroundColor Green
        Get-Content $LogFile -Tail $Lines
    }
    
    "search" {
        if ($Keyword -eq "") {
            Write-Host "❌ 请提供搜索关键词" -ForegroundColor Red
            Write-Host "用法: .\view-logs.ps1 -Action search -Keyword '关键词'" -ForegroundColor Yellow
            exit 1
        }
        Write-Host "🔍 搜索关键词: $Keyword" -ForegroundColor Green
        Get-Content $LogFile | Select-String $Keyword | Select-Object -Last $Lines
    }
    
    "ai" {
        Write-Host "🤖 显示 AI Agent 相关日志:" -ForegroundColor Green
        Get-Content $LogFile | Select-String "Agent|通义千问|🔍|🚀|📝|✅|⚠️|❌|💥|🔄" | Select-Object -Last $Lines
    }
    
    "error" {
        Write-Host "💥 显示错误日志:" -ForegroundColor Red
        Get-Content $LogFile | Select-String "ERROR|Exception|Failed|失败" | Select-Object -Last $Lines
    }
    
    "all" {
        Write-Host "📋 显示完整日志:" -ForegroundColor Green
        Get-Content $LogFile
    }
    
    "watch" {
        Write-Host "👀 实时监控日志 (按 Ctrl+C 退出):" -ForegroundColor Green
        Get-Content $LogFile -Wait -Tail $Lines
    }
    
    default {
        Write-Host @"
📖 使用说明:

基本用法:
  .\view-logs.ps1                          # 显示最新50行日志
  .\view-logs.ps1 -Lines 100               # 显示最新100行日志

专项查看:
  .\view-logs.ps1 -Action ai               # 查看AI相关日志
  .\view-logs.ps1 -Action error            # 查看错误日志
  .\view-logs.ps1 -Action watch            # 实时监控日志

搜索功能:
  .\view-logs.ps1 -Action search -Keyword "API"     # 搜索包含API的日志
  .\view-logs.ps1 -Action search -Keyword "用户问"   # 搜索用户问题

完整日志:
  .\view-logs.ps1 -Action all              # 显示完整日志

"@ -ForegroundColor Yellow
    }
}

Write-Host "`n📍 日志文件位置: $LogFile" -ForegroundColor Cyan
Write-Host "📊 文件大小: $((Get-Item $LogFile).Length / 1KB) KB" -ForegroundColor Cyan