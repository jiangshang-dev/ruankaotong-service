FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/index.html
COPY css /usr/share/nginx/html/css
COPY images /usr/share/nginx/html/images
COPY downloads /usr/share/nginx/html/downloads

EXPOSE 80
