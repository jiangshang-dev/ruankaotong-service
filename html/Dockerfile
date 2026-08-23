FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/index.html
COPY robots.txt /usr/share/nginx/html/robots.txt
COPY sitemap.xml /usr/share/nginx/html/sitemap.xml
COPY css /usr/share/nginx/html/css
COPY images /usr/share/nginx/html/images
COPY downloads /usr/share/nginx/html/downloads
COPY redishtml /usr/share/nginx/html/redishtml

RUN mkdir -p /etc/nginx/ssl

EXPOSE 80 443
