import apiClient from "./axios";

export const getRanking = async (period = "TODAY", limit = 7) => {
  const response = await apiClient.get("/rankings", {
    params: { period, limit },
  });
  return response.data;
};
