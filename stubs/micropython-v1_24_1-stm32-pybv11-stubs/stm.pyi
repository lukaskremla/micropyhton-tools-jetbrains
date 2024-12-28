"""
Functionality specific to STM32 MCUs.

MicroPython module: https://docs.micropython.org/en/v1.24.1/library/stm.html

This module provides functionality specific to STM32 microcontrollers, including
direct access to peripheral registers.

---
Module: 'stm' on micropython-v1.24.1-stm32-PYBV11
"""

# MCU: {'family': 'micropython', 'version': '1.24.1', 'build': '', 'ver': '1.24.1', 'port': 'stm32', 'board': 'PYBV11', 'cpu': 'STM32F405RG', 'mpy': 'v6.3', 'arch': 'armv7emsp'}
# Stubber: v1.24.0
from __future__ import annotations
from _typeshed import Incomplete
from typing import Tuple

SPI_I2SPR: int = 32
RTC_DR: int = 4
RTC_CR: int = 8
RTC_CALR: int = 60
RTC_ISR: int = 12
RTC_SSR: int = 40
RTC_SHIFTR: int = 44
RTC_PRER: int = 16
RTC_CALIBR: int = 24
RTC_BKP5R: int = 100
RTC_BKP4R: int = 96
RTC_BKP3R: int = 92
RTC_BKP6R: int = 104
RTC_BKP9R: int = 116
RTC_BKP8R: int = 112
RTC_BKP7R: int = 108
RTC_TAFCR: int = 64
SPI_CR1: int = 0
SPI3: int = 1073757184
SPI2: int = 1073756160
SPI_CR2: int = 4
SPI_I2SCFGR: int = 28
SPI_DR: int = 12
SPI_CRCPR: int = 16
SPI1: int = 1073819648
RTC_TSSSR: int = 56
RTC_TSDR: int = 52
RTC_TR: int = 0
RTC_TSTR: int = 48
SDIO: int = 1073818624
RTC_WUTR: int = 20
RTC_WPR: int = 36
RTC_BKP2R: int = 88
RNG: int = 1342572544
RCC_SSCGR: int = 128
RCC_PLLI2SCFGR: int = 132
RNG_CR: int = 0
RTC: int = 1073752064
RNG_SR: int = 4
RNG_DR: int = 8
RCC_PLLCFGR: int = 4
RCC_BDCR: int = 112
RCC_APB2RSTR: int = 36
RCC_APB2LPENR: int = 100
RCC_CFGR: int = 8
RCC_CSR: int = 116
RCC_CR: int = 0
RCC_CIR: int = 12
RTC_ALRMAR: int = 28
RTC_BKP16R: int = 144
RTC_BKP15R: int = 140
RTC_BKP14R: int = 136
RTC_BKP17R: int = 148
RTC_BKP1R: int = 84
RTC_BKP19R: int = 156
RTC_BKP18R: int = 152
RTC_BKP13R: int = 132
RTC_ALRMBSSR: int = 72
RTC_ALRMBR: int = 32
RTC_ALRMASSR: int = 68
RTC_BKP0R: int = 80
RTC_BKP12R: int = 128
RTC_BKP11R: int = 124
RTC_BKP10R: int = 120
WWDG_SR: int = 8
SPI_RXCRCR: int = 20
TIM_RCR: int = 48
TIM_PSC: int = 40
TIM_OR: int = 80
TIM_SMCR: int = 8
UART5: int = 1073762304
UART4: int = 1073761280
TIM_SR: int = 16
TIM_EGR: int = 20
TIM_CR1: int = 0
TIM_CNT: int = 36
TIM_CCR4: int = 64
TIM_CR2: int = 4
TIM_DMAR: int = 76
TIM_DIER: int = 12
TIM_DCR: int = 72
USART1: int = 1073811456
USB_OTG_FS: int = 1342177280
USART_SR: int = 0
USART_GTPR: int = 24
USB_OTG_HS: int = 1074003968
WWDG_CR: int = 0
WWDG_CFR: int = 4
WWDG: int = 1073753088
USART_DR: int = 4
USART6: int = 1073812480
USART3: int = 1073760256
USART2: int = 1073759232
USART_BRR: int = 8
USART_CR3: int = 20
USART_CR2: int = 16
USART_CR1: int = 12
TIM_CCR3: int = 60
TIM1: int = 1073807360
SYSCFG_PMC: int = 4
SYSCFG_MEMRMP: int = 0
TIM10: int = 1073824768
TIM13: int = 1073748992
TIM12: int = 1073747968
TIM11: int = 1073825792
SYSCFG_EXTICR3: int = 20
SYSCFG: int = 1073821696
SPI_TXCRCR: int = 24
SPI_SR: int = 8
SYSCFG_CMPCR: int = 32
SYSCFG_EXTICR2: int = 16
SYSCFG_EXTICR1: int = 12
SYSCFG_EXTICR0: int = 8
TIM14: int = 1073750016
TIM_CCER: int = 32
TIM_BDTR: int = 68
TIM_ARR: int = 44
TIM_CCMR1: int = 24
TIM_CCR2: int = 56
TIM_CCR1: int = 52
TIM_CCMR2: int = 28
TIM9: int = 1073823744
TIM4: int = 1073743872
TIM3: int = 1073742848
TIM2: int = 1073741824
TIM5: int = 1073744896
TIM8: int = 1073808384
TIM7: int = 1073746944
TIM6: int = 1073745920
EXTI_SWIER: int = 16
DAC_DOR1: int = 44
DAC_DHR8RD: int = 40
DAC_DHR8R2: int = 28
DAC_DOR2: int = 48
DBGMCU: int = 3758366720
DAC_SWTRIGR: int = 4
DAC_SR: int = 52
DAC_DHR8R1: int = 16
DAC_DHR12L2: int = 24
DAC_DHR12L1: int = 12
DAC_CR: int = 0
DAC_DHR12LD: int = 36
DAC_DHR12RD: int = 32
DAC_DHR12R2: int = 20
DAC_DHR12R1: int = 8
DBGMCU_APB1FZ: int = 8
EXTI_EMR: int = 4
EXTI: int = 1073822720
DMA_LISR: int = 0
EXTI_FTSR: int = 12
EXTI_RTSR: int = 8
EXTI_PR: int = 20
EXTI_IMR: int = 0
DMA_LIFCR: int = 8
DBGMCU_IDCODE: int = 0
DBGMCU_CR: int = 4
DBGMCU_APB2FZ: int = 12
DMA1: int = 1073897472
DMA_HISR: int = 4
DMA_HIFCR: int = 12
DMA2: int = 1073898496
DAC1: int = 1073771520
ADC_JDR3: int = 68
ADC_JDR2: int = 64
ADC_JDR1: int = 60
ADC_JDR4: int = 72
ADC_JOFR3: int = 28
ADC_JOFR2: int = 24
ADC_JOFR1: int = 20
ADC_HTR: int = 36
ADC2: int = 1073815808
ADC123_COMMON: int = 1073816320
ADC1: int = 1073815552
ADC3: int = 1073816064
ADC_DR: int = 76
ADC_CR2: int = 8
ADC_CR1: int = 4
ADC_JOFR4: int = 32
CRC: int = 1073885184
CAN2: int = 1073768448
CAN1: int = 1073767424
CRC_CR: int = 8
DAC: int = 1073771520
CRC_IDR: int = 4
CRC_DR: int = 0
ADC_SR: int = 0
ADC_SMPR1: int = 12
ADC_LTR: int = 40
ADC_JSQR: int = 56
ADC_SMPR2: int = 16
ADC_SQR3: int = 52
ADC_SQR2: int = 48
ADC_SQR1: int = 44
RCC_APB2ENR: int = 68
FLASH: int = 1073888256
IWDG: int = 1073754112
I2S3EXT: int = 1073758208
I2S2EXT: int = 1073755136
IWDG_KR: int = 0
IWDG_SR: int = 12
IWDG_RLR: int = 8
IWDG_PR: int = 4
I2C_TRISE: int = 32
I2C_DR: int = 16
I2C_CR2: int = 4
I2C_CR1: int = 0
I2C_OAR1: int = 8
I2C_SR2: int = 24
I2C_SR1: int = 20
I2C_OAR2: int = 12
PWR: int = 1073770496
RCC_AHB3LPENR: int = 88
RCC_AHB3ENR: int = 56
RCC_AHB2RSTR: int = 20
RCC_AHB3RSTR: int = 24
RCC_APB1RSTR: int = 32
RCC_APB1LPENR: int = 96
RCC_APB1ENR: int = 64
RCC_AHB2LPENR: int = 84
RCC: int = 1073887232
PWR_CSR: int = 4
PWR_CR: int = 0
RCC_AHB1ENR: int = 48
RCC_AHB2ENR: int = 52
RCC_AHB1RSTR: int = 16
RCC_AHB1LPENR: int = 80
I2C_CCR: int = 28
GPIOD: int = 1073875968
GPIOC: int = 1073874944
GPIOB: int = 1073873920
GPIOE: int = 1073876992
GPIOH: int = 1073880064
GPIOG: int = 1073879040
GPIOF: int = 1073878016
GPIOA: int = 1073872896
FLASH_KEYR: int = 4
FLASH_CR: int = 16
FLASH_ACR: int = 0
FLASH_OPTCR: int = 20
FLASH_SR: int = 12
FLASH_OPTKEYR: int = 8
FLASH_OPTCR1: int = 24
GPIOI: int = 1073881088
GPIO_OTYPER: int = 4
GPIO_OSPEEDR: int = 8
GPIO_ODR: int = 20
GPIO_PUPDR: int = 12
I2C3: int = 1073765376
I2C2: int = 1073764352
I2C1: int = 1073763328
GPIO_MODER: int = 0
GPIO_BSRR: int = 24
GPIO_AFR1: int = 36
GPIO_AFR0: int = 32
GPIO_BSRRH: int = 26
GPIO_LCKR: int = 28
GPIO_IDR: int = 16
GPIO_BSRRL: int = 24
mem32: Incomplete  ## <class 'mem'> = <32-bit memory>
mem8: Incomplete  ## <class 'mem'> = <8-bit memory>
mem16: Incomplete  ## <class 'mem'> = <16-bit memory>
