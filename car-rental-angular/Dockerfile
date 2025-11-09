# Multi-stage Dockerfile for Angular app
# Builder stage: Build the Angular app
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first for better caching
COPY car-rental-angular/package*.json ./

# Install dependencies (clean install for Linux)
RUN npm ci --no-optional

# Copy source code
COPY car-rental-angular/ .

# Clean any existing node_modules and reinstall for the correct platform
RUN rm -rf node_modules && npm ci

# Build for production
RUN npm run build

# Production stage: Serve with Nginx
FROM nginx:alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY <<EOF /etc/nginx/conf.d/default.conf
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html/browser;
    index index.html;

    # Handle Angular routes
    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # API proxy to backend service
    location /api/ {
        proxy_pass http://carrental-service:8080/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # WebSocket proxy for real-time features
    location /ws {
        proxy_pass http://carrental-service:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/javascript application/xml+rss application/json;
}
EOF

# Copy built Angular app
COPY --from=builder /app/dist/car-rental-angular/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]