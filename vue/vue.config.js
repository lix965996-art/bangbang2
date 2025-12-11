module.exports = {
  lintOnSave: false, // 关闭语法检查
  devServer: {
    host: '0.0.0.0',
    port: 8080,
    open: true,       // 运行后自动打开浏览
    disableHostCheck: true, 
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
  }
}