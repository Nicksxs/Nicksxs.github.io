tmp_buf = mmap(file, len);
write(socket, tmp_buf, len);