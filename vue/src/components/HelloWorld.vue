<template>
  <div class="hello">
    <h1>{{ msg }}</h1>
    <p>Welcome to the Agricultural IoT Platform</p>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'HelloWorld',
  props: {
    msg: {
      type: String,
      required: true
    }
  },
  setup() {
    return {}
  }
})
</script>

<style scoped>
.hello {
  text-align: center;
  padding: 20px;
}
</style>