# Étape 1 : build de l'application Angular
# (Angular 22 exige Node >= 22.22.3, d'où node:22 et non node:20)
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Étape 2 : servir les fichiers statiques avec nginx
FROM nginx:alpine
# Hôte de l'API Gateway (nom du service dans docker-compose)
ENV BACKEND_HOST=gateway-service
# envsubst ne remplace que BACKEND_HOST dans les templates
ENV NGINX_ENVSUBST_FILTER=^BACKEND_HOST$
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
