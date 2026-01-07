<template>
  <QuizEditorView 
    mode="edit" 
    :quizId="Number(route.params.id)"
  />
</template>

<script setup lang="ts">
import { useRoute } from "vue-router";
import QuizEditorView from "./QuizEditorView.vue";

const route = useRoute();
</script>
