val payloads = listOf(
    "HEAD / HTTP/1.1\r\nHost: #host#\r\nConnection: close\r\n\r\n",
    "USER anonymous\nSYST\nSTAT\nLIST\n",
    "admin\nadmin\nadmin\npassword\nroot\nroot\ncisco\ncisco\ntest\ntest\n\n\n",
    "EHLO #host#\nHELP\n",
    "USER root\nPASS root\nLIST\nRETR 1\n",
    "\u00AA\u00AA\u0001\u0000\u0000\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0007\u0065\u0078\u0061\u006d\u0070\u006c\u0065\u0003\u0063\u006f\u006d\u0000\u0000\u0001\u0000\u0001"
)
