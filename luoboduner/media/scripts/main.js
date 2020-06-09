new Vue({
  el: '#app',
  data() {
    return {
      scrolled: false,
      showToc: false,
      showMenu: false,
    } 
  },
  computed: {
    text() {
      return encodeURIComponent(document.title) 
    },
    url() {
      return encodeURIComponent(window.location.href)
    }
  },
  mounted() {
    window.addEventListener('scroll', this.navOnScroll)
  },
  methods: {
    oepnUrl(url) {
      window.open(url, '_blank')
    },
    navOnScroll() {
      if (window.scrollY > 20) {
        this.scrolled = true
      } else {
        this.scrolled = false
      }
    },
    backToUp() {
      window.scrollTo(0, 0)
    },
    shareToTwitter() {
      window.open(`https://twitter.com/share?text=${this.text}&url=${this.url}`, '_blank', 'width=615,height=505')
    },
    shareToWeibo() {
      window.open(`https://service.weibo.com/share/share.php?title=${this.text}&url=${this.url}`, '_blank', 'width=615,height=505')
    },
    shareToTelegram() {
      window.open(`https://telegram.me/share/url?text=${this.text}&url=${this.url}`, '_blank', 'width=615,height=505')
    },
  },
})