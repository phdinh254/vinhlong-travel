# Frontend React: build ra file tĩnh rồi phục vụ bằng nginx; nginx chuyển /api, /uploads sang backend
FROM node:22-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# Vite gắn biến VITE_* vào lúc build. Mặc định trỏ về chính nginx (cổng 8080) nên không cần CORS.
# shortcut: chỉ đúng khi mở trang ở http://localhost:8080; chạy ở tên miền khác thì build lại với VITE_API_URL tương ứng
ARG VITE_API_URL=http://localhost:8080
ARG VITE_GOOGLE_CLIENT_ID=
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 80
