import { getToken } from "./authService";

const BASE_URL = "http://localhost:8080/api/reports";

function getHeaders() {
  return {
    Authorization: `Bearer ${getToken()}`,
  };
}

export async function fetchReport({ from, to, department, employeeId }) {
  const params = new URLSearchParams({ from, to });
  if (department && department !== "all") params.append("department", department);
  if (employeeId) params.append("employeeId", employeeId);

  const res = await fetch(`${BASE_URL}?${params}`, {
    headers: getHeaders(),
  });
  if (!res.ok) throw new Error("Грешка при вчитување на извештај.");
  return res.json();
}

export async function importCsv(file) {
  const formData = new FormData();
  formData.append("file", file);

  const res = await fetch(`${BASE_URL}/import`, {
    method: "POST",
    headers: { Authorization: `Bearer ${getToken()}` },
    body: formData,
  });
  const text = await res.text();
  if (!res.ok) throw new Error(text);
  return text;
}