declare module "*.vue" {
  import { DefineComponent } from "vue";
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

declare module "*.vue" {
  import { DefineComponent } from "vue";
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

declare module "vue/types/options" {
  interface ComponentOptions<V extends Vue> {
    [key: string]: any;
  }
}

declare module "vue/types/vue" {
  interface Vue {
    [key: string]: any;
  }
}

declare module "vue/types/vnode" {
  interface VNodeData {
    [key: string]: any;
  }
}

declare module "*.css" {
  const content: string;
  export default content;
}

declare module "*.scss" {
  const content: string;
  export default content;
}

declare module "*.sass" {
  const content: string;
  export default content;
}

declare module "*.less" {
  const content: string;
  export default content;
}

declare module "*.styl" {
  const content: string;
  export default content;
}

declare module "*.svg" {
  const content: string;
  export default content;
}

declare module "*.png" {
  const content: string;
  export default content;
}

declare module "*.jpg" {
  const content: string;
  export default content;
}

declare module "*.jpeg" {
  const content: string;
  export default content;
}

declare module "*.gif" {
  const content: string;
  export default content;
}

declare module "*.bmp" {
  const content: string;
  export default content;
}

declare module "*.tiff" {
  const content: string;
  export default content;
}

declare module "*.webp" {
  const content: string;
  export default content;
}

declare module "*.avif" {
  const content: string;
  export default content;
}

declare module "*.json" {
  const content: any;
  export default content;
}

declare module "element-plus" {
  export * from "element-plus/es";
}

declare module "element-plus/es" {
  export * from "element-plus/es";
}

declare module "element-plus/es/components/button/index" {
  import { Button } from "element-plus";
  export default Button;
}

declare module "element-plus/es/components/input/index" {
  import { Input } from "element-plus";
  export default Input;
}

declare module "element-plus/es/components/form/index" {
  import { Form } from "element-plus";
  export default Form;
}

declare module "element-plus/es/components/table/index" {
  import { Table } from "element-plus";
  export default Table;
}

declare module "element-plus/es/components/pagination/index" {
  import { Pagination } from "element-plus";
  export default Pagination;
}

declare module "element-plus/es/components/message/index" {
  import { ElMessage } from "element-plus";
  export default ElMessage;
}

declare module "element-plus/es/components/message-box/index" {
  import { ElMessageBox } from "element-plus";
  export default ElMessageBox;
}

declare module "element-plus/es/components/dialog/index" {
  import { Dialog } from "element-plus";
  export default Dialog;
}