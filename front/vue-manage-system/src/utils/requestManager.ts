class RequestManager {
  private controllers: AbortController[] = [];

  // 添加新的 AbortController 实例
  add(controller: AbortController) {
    this.controllers.push(controller);
  }

  // 中止所有请求，并清空控制器列表
  abortAll() {
    this.controllers.forEach(controller => controller.abort());
    this.controllers = [];
  }
}

export const requestManager = new RequestManager();
