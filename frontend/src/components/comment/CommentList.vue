<!-- src/components/comment/CommentList.vue -->

<template>
  <div class="comment-list">
    <div v-if="comments.length === 0" class="text-center text-gray-400 py-4">
      아직 댓글이 없습니다.
    </div>

    <CommentItem
      v-for="(c, index) in comments"
      :key="c.id ?? c.commentId ?? index" 
      :comment="c"
      @updated="emit('updated')"
    />
  </div>
</template>

<script setup>
import CommentItem from "./CommentItem.vue";

const props = defineProps({
  comments: Array,
});

const emit = defineEmits(["updated"]);
</script>


<style scoped>
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
