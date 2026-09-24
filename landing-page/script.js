// EcoConnect Landing Page Interactivity
document.addEventListener('DOMContentLoaded', () => {
    console.log('EcoConnect Landing Page Cargada Exitosamente.');

    // Smooth Scroll para los enlaces del menú
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth'
                });
            }
        });
    });

    // Tracking simple del botón de descarga del APK
    const downloadBtns = document.querySelectorAll('a[href*="EcoConnect.apk"]');
    downloadBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            console.log('Iniciando descarga de APK...');
        });
    });
});
